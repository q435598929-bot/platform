package com.platform.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.platform.domain.ApplicationDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class ApplicationRegistry {
    private final Path registryPath;
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    private volatile Map<String, ApplicationDefinition> applications = Map.of();

    public ApplicationRegistry(@Value("${platform.registry-file}") String file) {
        registryPath = Path.of(file).toAbsolutePath().normalize();
        reload();
    }

    public synchronized void reload() {
        try {
            RegistryDocument document = mapper.readValue(registryPath.toFile(), RegistryDocument.class);
            Map<String, ApplicationDefinition> loaded = new LinkedHashMap<>();
            for (ApplicationDefinition app : document.getApplications()) {
                validate(app);
                if (loaded.putIfAbsent(app.getId(), app) != null) throw new IllegalStateException("Duplicate application id: " + app.getId());
            }
            applications = Map.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load registry " + registryPath + ": " + e.getMessage(), e);
        }
    }

    public List<ApplicationDefinition> findAll() {
        return applications.values().stream()
                .sorted(Comparator.comparingInt(ApplicationDefinition::getSortOrder)
                        .thenComparing(ApplicationDefinition::getId))
                .toList();
    }
    public ApplicationDefinition require(String id) {
        ApplicationDefinition app = applications.get(id);
        if (app == null) throw new IllegalArgumentException("Unknown application: " + id);
        return app;
    }
    public Path path() { return registryPath; }

    private void validate(ApplicationDefinition app) {
        if (app.getId() == null || !app.getId().matches("[a-z0-9][a-z0-9-]*")) throw new IllegalStateException("Invalid application id: " + app.getId());
        if (app.getName() == null || app.getName().isBlank()) throw new IllegalStateException("Application name is required: " + app.getId());
        if (app.getSourceRoot() == null || !Files.isDirectory(Path.of(app.getSourceRoot()))) throw new IllegalStateException("Source root does not exist: " + app.getSourceRoot());
        if (app.getComponents() == null || app.getComponents().isEmpty()) throw new IllegalStateException("At least one component is required: " + app.getId());
    }

    public static class RegistryDocument {
        private List<ApplicationDefinition> applications = List.of();
        public List<ApplicationDefinition> getApplications() { return applications; }
        public void setApplications(List<ApplicationDefinition> applications) { this.applications = applications; }
    }
}
