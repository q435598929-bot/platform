package com.platform.task.controller.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** Named --key=value arguments for page adapters; plain legacy arguments remain untouched. */
public final class TaskArguments {
    private TaskArguments() {}

    public static Map<String, String> named(String[] arguments) {
        Map<String, String> values = new LinkedHashMap<>();
        if (arguments == null) return values;
        for (String argument : arguments) {
            if (argument == null) continue;
            String normalized = argument.startsWith("--") ? argument.substring(2) : argument;
            int separator = normalized.indexOf('=');
            if (separator > 0) values.put(normalized.substring(0, separator).trim(), normalized.substring(separator + 1).trim());
        }
        return values;
    }

    public static String value(Map<String, String> values, String key, String legacyDefault) {
        String value = values.get(key);
        return value == null || value.isBlank() ? legacyDefault : value;
    }
}
