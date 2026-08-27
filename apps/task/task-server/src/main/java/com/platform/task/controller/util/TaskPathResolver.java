package com.platform.task.controller.util;

import java.nio.file.Path;

/** Adds container-friendly path overrides while preserving every legacy default. */
public final class TaskPathResolver {
    private TaskPathResolver() {}

    public static Path path(String environmentName, String legacyPath) {
        return Path.of(value(environmentName, legacyPath));
    }

    public static Path path(String environmentName, String legacyDirectory, String legacyFile) {
        String override = override(environmentName);
        return override == null || override.isBlank()
                ? Path.of(legacyDirectory, legacyFile)
                : Path.of(override);
    }

    public static String value(String environmentName, String legacyValue) {
        String override = override(environmentName);
        return override == null || override.isBlank() ? legacyValue : override;
    }

    private static String override(String environmentName) {
        String pageKey = switch (environmentName) {
            case "TASK_INPUT_PATH" -> "inputPath";
            case "TASK_AUX_INPUT_PATH" -> "auxInputPath";
            case "TASK_OUTPUT_DIR" -> "outputDir";
            default -> environmentName;
        };
        String pageValue = TaskExecutionContext.value(pageKey);
        return pageValue == null || pageValue.isBlank() ? System.getenv(environmentName) : pageValue;
    }
}
