package org.hyperagents.jacamo.artifacts.wot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;

/**
 * A CArtAgO artifact that can interpret a W3C WoT Thing Description (TD) and exposes the affordances
 * of the described Thing to agents. The artifact uses the hypermedia controls provided in the TD to
 * compose and issue HTTP requests for the exposed affordances.
 *
 * Supports:
 * - readThingProperty(URI, output) — one-shot property read
 * - observeThingProperty(URI, obsName) — subscribe to observable properties via HTTP long-polling
 * - invokeThingAction(URI, payload) — invoke actions on Things
 * - getThingActionPayload(URI, output) — describe the expected input schema for an action
 *
 * Contributors:
 * - Andrei Ciortea (author), Interactions-HSG, University of St.Gallen
 *
 */
public class ThingArtifact extends Artifact {
  private static final String WEBID_PREFIX = "http://hyperagents.org/";

  protected ThingDescription td;
  protected Optional<String> agentWebId;
  protected boolean dryRun;
  private Optional<String> apiKey;
  private final List<Thread> observeThreads = new CopyOnWriteArrayList<>();

  /**
   * Method called by CArtAgO to initialize the artifact. The W3C WoT Thing Description (TD) used by
   * this artifact is retrieved and parsed during initialization.
   *
   * @param url A URL that dereferences to a W3C WoT Thing Description.
   */
  public void init(String url) {
    int maxRetries = 10;
    int retryDelayMs = 2000;
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, url);
        break;
      } catch (IOException e) {
        if (attempt == maxRetries) {
          throw new RuntimeException("Failed to initialize ThingArtifact from " + url
              + " after " + maxRetries + " attempts: " + e.getMessage(), e);
        }
        System.out.println("[ThingArtifact] Attempt " + attempt + "/" + maxRetries
            + " failed for " + url + ", retrying in " + retryDelayMs + "ms...");
        try { Thread.sleep(retryDelayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
      }
    }

    for (SecurityScheme scheme : td.getSecuritySchemes()) {
      defineObsProperty("securityScheme", scheme.getConfiguration());
    }

    this.agentWebId = Optional.empty();
    this.apiKey = Optional.empty();
    this.dryRun = false;
  }

  /**
   * Method called by CArtAgO to initialize the artifact. The W3C WoT Thing Description (TD) used by
   * this artifact is retrieved and parsed during initialization.
   *
   * @param url A URL that dereferences to a W3C WoT Thing Description.
   * @param dryRun When set to true, the requests are logged, but not executed.
   */
  public void init(String url, boolean dryRun) {
    init(url);
    this.dryRun = dryRun;
  }

  /**
   * Called when the artifact is disposed. Interrupts all observe threads.
   */
  protected void dispose() {
    for (Thread t : observeThreads) {
      t.interrupt();
    }
    observeThreads.clear();
  }

  /**
   * CArtAgO operation for setting the WebID of an operating agent using the artifact.
   *
   * @param webId The operating agent's WebID as a string.
   */
  @OPERATION
  public void setOperatorWebId(String webId) {
    this.agentWebId = Optional.of(webId);
  }

  /**
   * CArtAgO operation for reading a property of a Thing using a semantic model of the Thing.
   *
   * @param propertyTag Either an IRI that identifies the property type, or the property's name.
   * @param output The read value. Can be a list of one or more primitives, or a nested list of
   * primitives or arbitrary depth.
   */
  @OPERATION
  public void readThingProperty(String propertyTag, OpFeedbackParam<Object[]> output) {
    readThingProperty(propertyTag, Optional.empty(), output);
  }

  /**
   * CArtAgO operation for reading a property of a Thing using a semantic model of the Thing.
   *
   * @param propertyTag Either an IRI that identifies the property type, or the property's name.
   * @param payloadTags A list of IRIs or object property names (if property is an object schema).
   * @param output The read value. Can be a list of one or more primitives, or a nested list of
   * primitives or arbitrary depth.
   */
  @OPERATION
  public void readThingProperty(String propertyTag, OpFeedbackParam<Object[]> payloadTags,
      OpFeedbackParam<Object[]> output) {
    readThingProperty(propertyTag, Optional.of(payloadTags), output);
  }

  /**
   * CArtAgO operation for observing a property of a Thing. Creates a CArtAgO observable property
   * that is kept up-to-date via HTTP long-polling (if the TD declares an observeProperty form)
   * or via periodic polling as a fallback.
   *
   * @param propertyTag Either an IRI that identifies the property type, or the property's name.
   */
  @OPERATION
  public void observeThingProperty(String propertyTag, String obsName) {
    PropertyAffordance property = getPropertyOrFail(propertyTag);

    // Read the initial value
    String initialValue = readPropertyValue(property);

    defineObsProperty(obsName, initialValue);

    // Check if there is an observeProperty form (long-polling)
    Optional<Form> observeForm = property.getFirstFormForOperationType(TD.observeProperty);

    if (observeForm.isPresent()) {
      String target = observeForm.get().getTarget();
      log("Starting long-poll observe for " + obsName + " at " + target);
      startLongPollThread(obsName, target);
    } else if (property.isObservable()) {
      // Fallback: poll every 5 seconds
      log("Starting polling observe for " + obsName);
      startPollingThread(obsName, property);
    } else {
      log("Property " + obsName + " is not observable, initial value set only");
    }
  }

  /**
   * CArtAgO operation for writing a property of a Thing using a semantic model of the Thing.
   *
   * @param propertyTag Either an IRI that identifies the property type, or the property's name.
   * @param payload The payload to be issued when writing the property.
   */
  @OPERATION
  public void writeProperty(String propertyTag, Object[] payload) {
    writeProperty(propertyTag, new Object[0], payload);
  }

  /**
   * CArtAgO operation for writing a property of a Thing using a semantic model of the Thing.
   *
   * @param propertyTag Either an IRI that identifies the property type, or the property's name.
   * @param payloadTags A list of IRIs or object property names (if property is an object schema).
   * @param payload The payload to be issued when writing the property.
   */
  @OPERATION
  public void writeProperty(String propertyTag, Object[] payloadTags, Object[] payload) {
    validateParameters(propertyTag, payloadTags, payload);
    if (payload.length == 0) {
      failed("The payload used when writing a property cannot be empty.");
    }

    PropertyAffordance property = getPropertyOrFail(propertyTag);
    Optional<TDHttpResponse> response = executePropertyRequest(property, TD.writeProperty,
        payloadTags, payload);

    if (response.isPresent() && !requestSucceeded(response.get().getStatusCode())) {
      failed("Status code: " + response.get().getStatusCode());
    }
  }


  @OPERATION
  public void invokeThingAction(String actionTag) {
    invokeThingAction(actionTag, new Object[0], new Object[0]);
  }

  @OPERATION
  public void invokeThingActionWithIntegerOutput(String semanticType, OpFeedbackParam<Integer> output) {
    OpFeedbackParam<Object[]> payload = new OpFeedbackParam<>();
    invokeThingAction(semanticType, new Object[0], new Object[0], payload);

    Object[] result = payload.get();
    if (result.length > 0) {
      output.set(((Integer) result[0]));
    }
  }

  /**
   * CArtAgO operation for invoking an action on a Thing using a semantic model of the Thing.
   *
   * @param actionTag Either an IRI that identifies the action type, or the action's name.
   * @param payload The payload to be issued when invoking the action.
   */
  @OPERATION
  public void invokeThingAction(String actionTag, Object[] payload) {
    invokeThingAction(actionTag, new Object[0], payload);
  }

  /**
   * CArtAgO operation for invoking an action on a Thing using a semantic model of the Thing.
   *
   * @param actionTag Either an IRI that identifies the action type, or the action's name.
   * @param payloadTags A list of IRIs or object property names (used for object schema payloads).
   * @param payload The payload to be issued when invoking the action.
   */
  @OPERATION
  public void invokeThingAction(String actionTag, Object[] payloadTags, Object[] payload) {
    invokeThingAction(actionTag, payloadTags, payload, null);
  }

  /**
   * CArtAgO operation for invoking an action on a Thing using a semantic model of the Thing.
   *
   * @param actionTag Either an IRI that identifies the action type, or the action's name.
   * @param payloadTags A list of IRIs or object property names (used for object schema payloads).
   * @param payload The payload to be issued when invoking the action.
   * @param output The list of values of the response payload.
   */
  @OPERATION
  public void invokeThingAction(String actionTag, Object[] payloadTags, Object[] payload,
      OpFeedbackParam<Object[]> output) {

    Optional<ActionAffordance> action = td.getFirstActionBySemanticType(actionTag);

    if (!action.isPresent()) {
      action = td.getActionByName(actionTag);
    }

    if (!action.isPresent()) {
      failed("Unknown action: " + actionTag);
      return;
    }

    Optional<Form> form = action.get().getFirstForm();

    if (!form.isPresent()) {
      failed("Invalid TD: the invoked action does not have a valid form.");
    }

    Optional<DataSchema> inputSchema = action.get().getInputSchema();
    if (!inputSchema.isPresent() && payload.length > 0) {
      failed("This type of action does not take any input: " + actionTag);
    }

    Optional<TDHttpResponse> response = executeRequest(TD.invokeAction, form.get(), inputSchema,
      payloadTags, payload);

    if (!dryRun && response.isPresent()) {
      if (!requestSucceeded(response.get().getStatusCode())) {
        failed("Status code: " + response.get().getStatusCode());
      } else if (action.get().getOutputSchema().isPresent()) {
        readPayloadWithSchema(response.get(), action.get().getOutputSchema().get(), output);
      } else if (output != null) {
        readPayloadWithSchema(response.get(), DataSchema.getEmptySchema(), output);
      }
    }
  }

  /**
   * CArtAgO operation that returns a human-readable description of the expected input schema
   * for an action affordance.
   *
   * @param actionTag Either an IRI that identifies the action type, or the action's name.
   * @param output A string describing the expected input payload.
   */
  @OPERATION
  public void getThingActionPayload(String actionTag, OpFeedbackParam<String> output) {
    Optional<ActionAffordance> action = td.getFirstActionBySemanticType(actionTag);

    if (!action.isPresent()) {
      action = td.getActionByName(actionTag);
    }

    if (!action.isPresent()) {
      failed("Unknown action: " + actionTag);
      return;
    }

    Optional<DataSchema> inputSchema = action.get().getInputSchema();
    if (!inputSchema.isPresent()) {
      output.set("No input required");
      return;
    }

    output.set(schemaToString(inputSchema.get()));
  }

  /**
   * CArtAgO operation that sets an authentication token (used with APIKeySecurityScheme).
   *
   * @param token The authentication token.
   */
  @OPERATION
  public void setAPIKey(String token) {
    if (token != null && !token.isEmpty()) {
      this.apiKey = Optional.of(token);
    }
  }

  // --- Helper: convert a DataSchema to a human-readable string ---

  private String schemaToString(DataSchema schema) {
    StringBuilder sb = new StringBuilder();
    String datatype = schema.getDatatype();

    sb.append(datatype);

    // Show semantic types if available
    Set<String> types = schema.getSemanticTypes();
    if (types != null && !types.isEmpty()) {
      sb.append(" (");
      sb.append(String.join(", ", types));
      sb.append(")");
    }

    // Show enum values if available
    Set<String> enumValues = schema.getEnumeration();
    if (enumValues != null && !enumValues.isEmpty()) {
      sb.append(" enum{");
      List<String> vals = new ArrayList<>();
      for (Object v : enumValues) {
        vals.add(String.valueOf(v));
      }
      sb.append(String.join(", ", vals));
      sb.append("}");
    }

    // Recurse into object properties
    if (DataSchema.OBJECT.equals(datatype) && schema instanceof ObjectSchema) {
      ObjectSchema objSchema = (ObjectSchema) schema;
      Map<String, DataSchema> props = objSchema.getProperties();
      if (props != null && !props.isEmpty()) {
        sb.append(" { ");
        List<String> propDescs = new ArrayList<>();
        for (Map.Entry<String, DataSchema> entry : props.entrySet()) {
          propDescs.add(entry.getKey() + ": " + schemaToString(entry.getValue()));
        }
        sb.append(String.join(", ", propDescs));
        sb.append(" }");
      }
    }

    // Recurse into array items
    if (DataSchema.ARRAY.equals(datatype) && schema instanceof ArraySchema) {
      ArraySchema arrSchema = (ArraySchema) schema;
      List<DataSchema> items = arrSchema.getItems();
      if (items != null && !items.isEmpty()) {
        sb.append(" [");
        List<String> itemDescs = new ArrayList<>();
        for (DataSchema item : items) {
          itemDescs.add(schemaToString(item));
        }
        sb.append(String.join(", ", itemDescs));
        sb.append("]");
      }
    }

    return sb.toString();
  }

  // --- Helper: read a single primitive value from a property ---

  private String readPropertyValue(PropertyAffordance property) {
    Optional<TDHttpResponse> response = executePropertyRequest(property, TD.readProperty,
        new Object[0], new Object[0]);

    if (response.isPresent() && requestSucceeded(response.get().getStatusCode())) {
      return parsePrimitiveResponse(response.get(), property.getDataSchema());
    }
    return "";
  }

  private String parsePrimitiveResponse(TDHttpResponse response, DataSchema schema) {
    switch (schema.getDatatype()) {
      case DataSchema.BOOLEAN:
        return String.valueOf(response.getPayloadAsBoolean());
      case DataSchema.INTEGER:
        return String.valueOf(response.getPayloadAsInteger());
      case DataSchema.NUMBER:
        return String.valueOf(response.getPayloadAsDouble());
      case DataSchema.STRING:
      case DataSchema.DATA:
        return response.getPayloadAsString();
      default:
        return response.getPayloadAsString();
    }
  }

  // --- Helper: update an observable property, parsing numeric values when possible ---

  private void updateObsPropertyParsed(String obsName, String value) {
    beginExtSession();
    try {
      try {
        int intVal = Integer.parseInt(value);
        updateObsProperty(obsName, intVal);
      } catch (NumberFormatException e1) {
        try {
          double dblVal = Double.parseDouble(value);
          updateObsProperty(obsName, dblVal);
        } catch (NumberFormatException e2) {
          updateObsProperty(obsName, value);
        }
      }
    } finally {
      endExtSession();
    }
  }

  // --- Long-poll thread for observeProperty ---

  private void startLongPollThread(String obsName, String target) {
    Thread t = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          URL url = new URL(target);
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("GET");
          conn.setRequestProperty("Accept", "application/json");
          // Long-poll: the server holds the connection until a change occurs
          conn.setReadTimeout(0);

          int status = conn.getResponseCode();
          if (status >= 200 && status < 300) {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
              sb.append(line);
            }
            reader.close();

            String value = sb.toString().trim();
            // Strip surrounding quotes if present (JSON string)
            if (value.startsWith("\"") && value.endsWith("\"")) {
              value = value.substring(1, value.length() - 1);
            }

            updateObsPropertyParsed(obsName, value);
          } else {
            // Non-success status, back off
            Thread.sleep(5000);
          }
          conn.disconnect();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          // Connection error, back off and retry
          try {
            Thread.sleep(5000);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    });
    t.setDaemon(true);
    t.setName("observe-longpoll-" + obsName);
    observeThreads.add(t);
    t.start();
  }

  // --- Polling fallback for observable properties without observe form ---

  private void startPollingThread(String obsName, PropertyAffordance property) {
    Thread t = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Thread.sleep(5000);
          String value = readPropertyValue(property);
          updateObsPropertyParsed(obsName, value);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          // Ignore and retry
        }
      }
    });
    t.setDaemon(true);
    t.setName("observe-poll-" + obsName);
    observeThreads.add(t);
    t.start();
  }

  /* Set a primitive payload. */
  TDHttpRequest setPrimitivePayload(TDHttpRequest request, DataSchema schema, Object payload) {
    try {
      if (payload instanceof Boolean) {
        request.setPrimitivePayload(schema, (boolean) payload);
      } else if (payload instanceof Byte || payload instanceof Integer || payload instanceof Long) {
        request.setPrimitivePayload(schema, Long.valueOf(String.valueOf(payload)));
      } else if (payload instanceof Float || payload instanceof Double) {
        request.setPrimitivePayload(schema, Double.valueOf(String.valueOf(payload)));
      } else if (payload instanceof String) {
        request.setPrimitivePayload(schema, (String) payload);
      } else {
        failed("Unable to detect the primitive datatype of payload: "
            + payload.getClass().getCanonicalName());
      }
    } catch (IllegalArgumentException e) {
      failed(e.getMessage());
    }

    return request;
  }

  /* Set a TD ObjectSchema payload */
  TDHttpRequest setObjectPayload(TDHttpRequest request, DataSchema schema, Object[] tags,
      Object[] payload) {
    Map<String, Object> requestPayload = new HashMap<String, Object>();

    for (int i = 0; i < tags.length; i ++) {
      if (tags[i] instanceof String) {
        requestPayload.put((String) tags[i], payload[i]);
      }
    }

    request.setObjectPayload((ObjectSchema) schema, requestPayload);

    return request;
  }

  /* Set a TD ArraySchema payload */
  TDHttpRequest setArrayPayload(TDHttpRequest request, DataSchema schema, Object[] payload) {
    request.setArrayPayload((ArraySchema) schema, Arrays.asList(payload));
    return request;
  }

  /* Matches the entire 2XX class */
  private boolean requestSucceeded(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  private void validateParameters(String semanticType, Object[] tags, Object[] payload) {
    if (tags.length > 0 && tags.length != payload.length) {
      failed("Illegal arguments: the lists of tags and action parameters should have equal length.");
    }
  }

  private void readThingProperty(String semanticType, Optional<OpFeedbackParam<Object[]>> tags,
      OpFeedbackParam<Object[]> output) {
    PropertyAffordance property = getPropertyOrFail(semanticType);
    Optional<TDHttpResponse> response = executePropertyRequest(property, TD.readProperty,
        new Object[0], new Object[0]);

    if (!dryRun) {
      if (!response.isPresent()) {
        failed("Something went wrong with the read property request.");
      }

      if (requestSucceeded(response.get().getStatusCode())) {
        readPayloadWithSchema(response.get(), property.getDataSchema(), tags, output);
      } else {
        failed("Status code: " + response.get().getStatusCode());
      }
    }
  }

  /* Tries to retrieve a property first by semantic tag, then by name. Fails if none works. */
  private PropertyAffordance getPropertyOrFail(String propertyTag) {
    Optional<PropertyAffordance> property = td.getFirstPropertyBySemanticType(propertyTag);

    if (!property.isPresent()) {
      property = td.getPropertyByName(propertyTag);
    }

    if (!property.isPresent()) {
      failed("Unknown property: " + propertyTag);
    }

    return property.get();
  }

  @SuppressWarnings("unchecked")
  private void readPayloadWithSchema(TDHttpResponse response, DataSchema schema,
      OpFeedbackParam<Object[]> output) {

    switch (schema.getDatatype()) {
      case DataSchema.BOOLEAN:
        output.set(new Boolean[] { response.getPayloadAsBoolean() });
        break;
      case DataSchema.STRING:
      case DataSchema.DATA:
        output.set(new String[] { response.getPayloadAsString() });
        break;
      case DataSchema.INTEGER:
        output.set(new Integer[] { response.getPayloadAsInteger() });
        break;
      case DataSchema.NUMBER:
        output.set(new Double[] { response.getPayloadAsDouble() });
        break;
      case DataSchema.OBJECT:
        Map<String, Object> payload = response.getPayloadAsObject((ObjectSchema) schema);
        List<String> tagList = new ArrayList<String>();
        List<Object> data = new ArrayList<Object>();
        List<Object> preferredTags = new ArrayList<Object>();
        for (String tag : payload.keySet()) {
          if (preferredTags.contains(tag)){
            tagList.add(tag);
            Object value = payload.get(tag);
            if (value instanceof Collection<?>) {
              data.add(nestedListsToArrays((Collection<Object>) value));
            } else {
              data.add(value);
              }
          }
        }
        output.set(data.toArray());
        break;
      case DataSchema.ARRAY:
        List<Object> arrayPayload = response.getPayloadAsArray((ArraySchema) schema);
        output.set(nestedListsToArrays(arrayPayload));
        break;
      default:
        break;
    }
  }

  @SuppressWarnings("unchecked")
  private void readPayloadWithSchema(TDHttpResponse response, DataSchema schema,
      Optional<OpFeedbackParam<Object[]>> tags, OpFeedbackParam<Object[]> output) {

    switch (schema.getDatatype()) {
      case DataSchema.BOOLEAN:
        output.set(new Boolean[] { response.getPayloadAsBoolean() });
        break;
      case DataSchema.STRING:
        output.set(new String[] { response.getPayloadAsString() });
        break;
      case DataSchema.INTEGER:
        output.set(new Integer[] { response.getPayloadAsInteger() });
        break;
      case DataSchema.NUMBER:
        output.set(new Double[] { response.getPayloadAsDouble() });
        break;
      case DataSchema.OBJECT:
        if (tags.isPresent()) {
          Map<String, Object> payload = response.getPayloadAsObject((ObjectSchema) schema);
          List<String> tagList = new ArrayList<String>();
          List<Object> data = new ArrayList<Object>();

          for (String tag : payload.keySet()) {
            tagList.add(tag);
            Object value = payload.get(tag);
            if (value instanceof Collection<?>) {
              data.add(nestedListsToArrays((Collection<Object>) value));
            } else {
              data.add(value);
            }
          }

          tags.get().set(tagList.toArray());
          output.set(data.toArray());
        }
        break;
      case DataSchema.ARRAY:
        List<Object> payload = response.getPayloadAsArray((ArraySchema) schema);
        output.set(nestedListsToArrays(payload));
        break;
      default:
        break;
    }
  }

  @SuppressWarnings("unchecked")
  Object[] nestedListsToArrays(Collection<Object> data) {
    Object[] out = data.toArray();

    for (int i = 0; i < out.length; i ++) {
      if (out[i] instanceof Collection<?>) {
        out[i] = nestedListsToArrays((Collection<Object>) out[i]);
      }
    }

    return out;
  }

  private Optional<TDHttpResponse> executePropertyRequest(PropertyAffordance property,
    String operationType, Object[] tags, Object[] payload) {
    Optional<Form> form = property.getFirstFormForOperationType(operationType);

    if (!form.isPresent()) {
      failed("Invalid TD: the property does not have a valid form.");
    }

    DataSchema schema = property.getDataSchema();

    return executeRequest(operationType, form.get(), Optional.of(schema), tags, payload);
  }

  private Optional<TDHttpResponse> executeRequest(String operationType, Form form,
      Optional<DataSchema> schema, Object[] tags, Object[] payload) {
    if (schema.isPresent() && payload.length > 0) {
      if (tags.length >= 1) {
        return executeRequestObjectPayload(operationType, form, schema.get(), tags, payload);
      } else if (payload.length == 1 && !(payload[0] instanceof Object[])) {
        return executeRequestPrimitivePayload(operationType, form, schema.get(), payload[0]);
      } else if (payload.length == 1 && (payload[0] instanceof Object[])) {
        return executeRequestArrayPayload(operationType, form, schema.get(), (Object []) payload[0]);
      } else if (payload.length >= 1) {
        return executeRequestArrayPayload(operationType, form, schema.get(), payload);
      } else {
        failed("Could not detect the type of payload (primitive, object, or array).");
        return Optional.empty();
      }
    } else {
      TDHttpRequest request = new TDHttpRequest(form, operationType);
      return issueRequest(request);
    }
  }

  private Optional<TDHttpResponse> executeRequestPrimitivePayload(String operationType, Form form,
      DataSchema schema, Object payload) {
    TDHttpRequest request = new TDHttpRequest(form, operationType);
    request = setPrimitivePayload(request, schema, payload);

    return issueRequest(request);
  }

  private Optional<TDHttpResponse> executeRequestObjectPayload(String operationType, Form form,
      DataSchema schema, Object[] tags, Object[] payload) {
    if (schema.getDatatype() != DataSchema.OBJECT) {
      failed("TD mismatch: illegal arguments, this affordance uses a data schema of type "
          + schema.getDatatype());
    }

    TDHttpRequest request = new TDHttpRequest(form, operationType);
    request = setObjectPayload(request, schema, tags, payload);

    return issueRequest(request);
  }

  private Optional<TDHttpResponse> executeRequestArrayPayload(String operationType, Form form,
      DataSchema schema, Object[] payload) {
    if (schema.getDatatype() != DataSchema.ARRAY) {
      failed("TD mismatch: illegal arguments, this affordance uses a data schema of type "
          + schema.getDatatype());
    }

    TDHttpRequest request = new TDHttpRequest(form, operationType);
    request = setArrayPayload(request, schema, payload);

    return issueRequest(request);
  }

  private Optional<TDHttpResponse> issueRequest(TDHttpRequest request) {
    Optional<SecurityScheme> scheme = td.getFirstSecuritySchemeByType(WoTSec.APIKeySecurityScheme);

    if (scheme.isPresent() && apiKey.isPresent()) {
      request.setAPIKey((APIKeySecurityScheme) scheme.get(), apiKey.get());
    }

    if (agentWebId.isPresent()) {
      request.addHeader("X-Agent-WebID", agentWebId.get());
    } else {
      request.addHeader("X-Agent-WebID", WEBID_PREFIX + getCurrentOpAgentId().getAgentName());
    }
    request.addHeader("X-Agent-LocalName", getCurrentOpAgentId().getAgentName());

    if (this.dryRun) {
      log(request.toString());
      return Optional.empty();
    } else {
      log(request.toString());
      try {
        return Optional.of(request.execute());
      } catch (IOException e) {
        failed(e.getMessage());
      }
    }

    return Optional.empty();
  }
}
