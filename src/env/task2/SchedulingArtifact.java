package task2;

import cartago.Artifact;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;

/**
 * A SchedulingArtifact that beats every second and provides two operations:
 *
 * - setReminder(propName, value, beats):
 *     After the specified number of beats (seconds), adds an observable property
 *     with the given name and value. The observing agent perceives this as a new belief.
 *
 * - setRecurrentReminder(propName, value, beats):
 *     Every N beats (seconds), adds/re-adds an observable property with the given name and value.
 *     The agent gets re-notified each cycle.
 *
 * Hints:
 * - Use @OPERATION for operations callable by agents, @INTERNAL_OPERATION for internal async operations
 * - Use execInternalOp("opName", args...) to launch an internal operation from an @OPERATION
 * - Use defineObsProperty(name, value) to create a new observable property
 * - Use removeObsProperty(name) to remove an observable property
 * - Use getObsProperty(name) to check if a property exists (returns null if not)
 *
 * Note on @OPERATION vs @INTERNAL_OPERATION:
 * @OPERATION is blocking, @INTERNAL_OPERATION is not.
 *
 * Examples:
 * - @OPERATION: https://github.com/cake-lier/cartago-ex-05/tree/main
 * - @INTERNAL_OPERATION: https://github.com/cake-lier/cartago-ex-06/tree/main
 *
 */
public class SchedulingArtifact extends Artifact {

    private int beatCount;

    void init() {
        beatCount = 0;
        execInternalOp("beat");
    }

    @INTERNAL_OPERATION
    void beat() {
        while (true) {
            await_time(1000);
            beatCount++;
        }
    }

    /* Task 2.1 Start of your solution */
    // TODO: Implement setReminder(String propName, String value, int beats)
    // This operation should wait for the given number of beats,
    // then create an observable property.

    // TODO: Implement setRecurrentReminder(String propName, String value, int beats)
    // This operation should launch an internal operation that repeatedly waits for
    // the given number of beats, then creates/re-creates an observable property.
    // Hint: to re-notify the agent, remove the property first, then re-define it.

    /* Task 2.1 End of your solution */
}
