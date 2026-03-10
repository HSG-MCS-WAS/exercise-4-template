// train controller agent
// The train controller monitors the train ride and performs periodic guest checks.
// When the ride starts, the controller sets up a recurrent reminder to check guests
// every 2 seconds. The controller relies on the train driver to publish the end_ride
// reminder, which stops the guest checks.
//
// The SchedulingArtifact provides:
//   setReminder(propName, value, beats) — one-shot: after N beats (seconds), adds propName(value) as belief
//   setRecurrentReminder(propName, value, beats) — recurring: every N beats (seconds), re-adds propName(value)

/* Initial beliefs */

/* Initial goals */

!start.


/* Plans */

@start_plan
+!start : true <-
    .print("Train controller starting up...");

    /* Task 2.2 Start of your solution */
    // TODO: Create the SchedulingArtifact programmatically and focus on it.
    // See the EXERCISE.md description for the artifact name and class,
    // and the CArtAgO documentation for how to create and focus on artifacts.

    /* Task 2.2 End of your solution */

    .print("Train controller ready. Monitoring train rides.").

@start_retry
-!start [error(Error), error_msg(Msg)] : true <-
    .print("Start failed (", Error, " - ", Msg, "). Retrying in 5 seconds...");
    .wait(5000);
    !start.

// React to a train ride starting — start recurrent guest checks
@train_ride_plan
+train_ride(Destination) : true <-
    .print("Train ride to ", Destination, " has started. Beginning guest checks.");

    +train_ride_on;

    /* Task 2.5 Start of your solution */
    // TODO: Set up recurrent guest checks using the SchedulingArtifact.
    // See the EXERCISE.md description for the timing requirements.

    /* Task 2.5 End of your solution */
    .print("Recurrent guest checks set up for ride to ", Destination, ".").

// React to the recurrent guest check reminder (only when ride is ongoing)
@guest_check_plan
+guest_check("now") : train_ride_on <-
    .print("Checking tickets and assisting guests.").

// React to the end of the ride (reminder set by the train driver)
@end_ride_plan
+end_ride(Destination) : true <-
    .print("Train has arrived at ", Destination, ". All passengers please disembark.");
    -train_ride_on.

/* Import behavior of agents that work in CArtAgO environments */
{ include("$jacamoJar/templates/common-cartago.asl") }
