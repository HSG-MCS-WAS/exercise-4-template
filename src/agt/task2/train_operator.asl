// train operator agent
// The train operator schedules train rides by writing to the SchedulingArtifact.
//
// The SchedulingArtifact provides:
//   setReminder(propName, value, beats) — one-shot: after N beats (seconds), adds propName(value) as belief
//   setRecurrentReminder(propName, value, beats) — recurring: every N beats (seconds), re-adds propName(value)

/* Initial goals */

!start.

/* Plans */

@start_plan
+!start : true <-
    .print("Train operator starting up...");

    // Look up and focus on the scheduler artifact
    lookupArtifact("scheduler", SchedulerId);
    focus(SchedulerId);

    .print("Train operator ready.");

    /* Task 2.3 Start of your solution */
    // TODO: Schedule a train ride using the SchedulingArtifact.
    // See the EXERCISE.md description for the destination and departure time.

    /* Task 2.3 End of your solution */
    .print("Train ride to Bern scheduled for departure at time 5.").

@start_retry
-!start [error(Error), error_msg(Msg)] : true <-
    .print("Start failed (", Error, " - ", Msg, "). Retrying in 5 seconds...");
    .wait(5000);
    !start.

/* Import behavior of agents that work in CArtAgO environments */
{ include("$jacamoJar/templates/common-cartago.asl") }
