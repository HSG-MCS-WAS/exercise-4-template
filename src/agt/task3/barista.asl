// barista agent
// See the ThingArtifact API documentation in EXERCISE.md for how to interact with WoT Things.
// The ThingArtifacts are pre-configured in the .jcm file.
//
// The SchedulingArtifact provides:
//   setReminder(propName, value, beats) — one-shot: after N beats, adds propName(value) as belief
//   setRecurrentReminder(propName, value, beats) — recurring: every N beats, re-adds propName(value)
//
// The personal assistant's scheduler (named "scheduler") can be focused on to observe
// upcoming_event beliefs — any agent that focuses on it can react to these.
//
// The coffee machine is a ThingArtifact (named "coffee_machine"). Use invokeThingAction with
// the appropriate action URI to control it. See the coffee-machine.ttl TD for available actions and allowed states.

/* Initial goals */

!start.

/* Plans */

@start_plan
+!start : true <-
    .print("Barista agent starting up...");
    .wait(5000);
    .print("Barista agent ready.").

@start_retry
-!start [error(Error), error_msg(Msg)] : true <-
    .print("Start failed (", Error, " - ", Msg, "). Retrying in 5 seconds...");
    .wait(5000);
    !start.

/* Bonus 1 Start of your solution */
/* Bonus 1 End of your solution */

/* Bonus 2 Start of your solution */
/* Bonus 2 End of your solution */

/* Import behavior of agents that work in CArtAgO environments */
{ include("$jacamoJar/templates/common-cartago.asl") }
