package com.platform.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationRegistryTest {
    @Test
    void loadsWorkspaceRegistry() {
        ApplicationRegistry registry = new ApplicationRegistry("../../../registry/applications.yml");
        assertThat(registry.findAll()).extracting("id").containsExactly("platform", "ai", "task");
        assertThat(registry.require("ai").getComponents()).hasSize(2);
    }

    @Test
    void loadsComposeRegistry(@TempDir Path tempDirectory) throws IOException {
        Path sourceRoot = Path.of("../../..").toAbsolutePath().normalize();
        String yaml = Files.readString(Path.of("../../../registry/applications.compose.yml"))
                .replace("sourceRoot: /workspace", "sourceRoot: '" + sourceRoot + "'");
        Path hostReadableRegistry = Files.writeString(tempDirectory.resolve("applications.compose.yml"), yaml);
        ApplicationRegistry registry = new ApplicationRegistry(hostReadableRegistry.toString());
        assertThat(registry.findAll()).extracting("id").containsExactly("platform", "ai", "task");
        assertThat(registry.require("task").getStartCommand())
                .containsExactly("docker", "start", "platform-task-server-1", "platform-task-web-1");
    }
}
