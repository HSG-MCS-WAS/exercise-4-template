// personal assistant agent
// See the ThingArtifact API documentation in EXERCISE.md for how to interact with WoT Things.
// The ThingArtifacts are pre-configured and focused via the .jcm file.
// Available artifacts: wristband, event_portal, lights, blinds, mattress, scheduler
// Use ?focused(smart_room, artifact_name, ArtifactId) to retrieve an artifact's ID.

/* Initial goals */

!start.

/* Plans */

@start_plan
+!start : true <-
    .print("Waiting for WoT servient to be ready...");
    .wait(3000);
    .print("Hello from the personal assistant!").

@start_retry
-!start [error(Error), error_msg(Msg)] : true <-
    .print("Start failed (", Error, " - ", Msg, "). Retrying in 5 seconds...");
    .wait(5000);
    !start.

/* Task 3 Start of your solution */
/* Task 3 End of your solution */

/* Import behavior of agents that work in CArtAgO environments */
{ include("$jacamoJar/templates/common-cartago.asl") }
