// train driver agent
// The train driver listens for train rides, drives the train, and communicates
// the end of the ride. The driver also announces the upcoming arrival.
//
// The SchedulingArtifact provides:
//   setReminder(propName, value, beats) — one-shot: after N beats (seconds), adds propName(value) as belief
//   setRecurrentReminder(propName, value, beats) — recurring: every N beats (seconds), re-adds propName(value)

/* Initial beliefs */

// Travel durations from St. Gallen to major Swiss cities (in seconds, simulating minutes)
travel_duration("Bern", 20).
travel_duration("Zurich", 12).
travel_duration("Basel", 17).
travel_duration("Lucerne", 14).
travel_duration("Geneva", 28).

/* Initial goals */

!start.

/* Plans */

@start_plan
+!start : true <-
    .print("Train driver starting up...");

    // Look up and focus on the scheduler artifact
    lookupArtifact("scheduler", SchedulerId);
    focus(SchedulerId);

    .print("Train driver ready. Waiting for train rides.").

@start_retry
-!start [error(Error), error_msg(Msg)] : true <-
    .print("Start failed (", Error, " - ", Msg, "). Retrying in 5 seconds...");
    .wait(5000);
    !start.

// React to a train ride being scheduled
@train_ride_plan
+train_ride(Destination) : travel_duration(Destination, Duration) <-
    .print("Departing to ", Destination, "! Estimated travel time: ", Duration, " seconds.");

    /* Task 2.4 Start of your solution */
    // TODO: Use the SchedulingArtifact to schedule two reminders:
    // 1. A reminder for the driver to announce the upcoming arrival shortly before it
    // 2. A reminder signaling the end of the ride when the train arrives
    // See the EXERCISE.md description for the exact timing requirements.

    /* Task 2.4 End of your solution */
    .print("Reminders set. Driving the train...").

// React to the arrival announcement reminder
@announcement_plan
+announcement("now") : true <-
    .print("Announcing upcoming arrival to passengers.").

// React to the end of the ride
@end_ride_plan
+end_ride(Destination) : true <-
    .print("Arrived at ", Destination, "! End of ride.").

/* Import behavior of agents that work in CArtAgO environments */
{ include("$jacamoJar/templates/common-cartago.asl") }
