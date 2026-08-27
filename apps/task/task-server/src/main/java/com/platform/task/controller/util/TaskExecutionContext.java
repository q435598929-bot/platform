package com.platform.task.controller.util;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;

/** Page-owned inputs for one managed invocation. Direct main() execution has no context. */
public final class TaskExecutionContext implements AutoCloseable {
    private static final InheritableThreadLocal<State> CURRENT = new InheritableThreadLocal<>();
    private final State state;

    private TaskExecutionContext(Map<String, String> inputs) {
        state = new State(inputs == null ? Map.of() : Map.copyOf(inputs));
        CURRENT.set(state);
    }

    public static TaskExecutionContext open(Map<String, String> inputs) {
        return new TaskExecutionContext(inputs);
    }

    public static String value(String key) {
        State state = CURRENT.get();
        return state == null ? null : state.inputs.get(key);
    }

    public static void recordOutput(Map<String, Object> output) {
        State state = CURRENT.get();
        if (state != null) state.output = output == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(output));
    }

    public Map<String, Object> output() {
        return state.output;
    }

    @Override
    public void close() {
        CURRENT.remove();
    }

    private static final class State {
        private final Map<String, String> inputs;
        private Map<String, Object> output = Map.of();
        private State(Map<String, String> inputs) { this.inputs = inputs; }
    }
}
