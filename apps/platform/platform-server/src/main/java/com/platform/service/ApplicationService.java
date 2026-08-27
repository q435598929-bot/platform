package com.platform.service;

import com.platform.domain.ApplicationDefinition;
import com.platform.domain.ApplicationView;
import com.platform.registry.ApplicationRegistry;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ApplicationService {
    private final ApplicationRegistry registry;
    private final ApplicationCatalogPersistence persistence;

    public ApplicationService(ApplicationRegistry registry, ApplicationCatalogPersistence persistence) {
        this.registry = registry;
        this.persistence = persistence;
    }

    public List<ApplicationView> list() { return registry.findAll().stream().map(this::view).toList(); }
    public ApplicationView get(String id) { return view(registry.require(id)); }
    public ApplicationView start(String id) { executeLifecycle(registry.require(id), Operation.START); return get(id); }
    public ApplicationView stop(String id) { executeLifecycle(registry.require(id), Operation.STOP); return get(id); }
    public void reload() { registry.reload(); persistence.synchronize(); }

    private ApplicationView view(ApplicationDefinition app) {
        Check check = status(app);
        return new ApplicationView(app.getId(), app.getSortOrder(), app.getName(), app.getDescription(), app.getCategory(), app.getSourceRoot(),
                app.getRuntime(), app.isLifecycleEnabled(), check.status(), check.detail(), app.getLinks(), app.getComponents());
    }

    private Check status(ApplicationDefinition app) {
        if (!app.isLifecycleEnabled()) return new Check(ApplicationView.Status.DISABLED, "Lifecycle control not enabled");
        if (app.getStatusCommand().isEmpty()) return new Check(ApplicationView.Status.UNKNOWN, "No status command");
        try {
            Result result = execute(app, app.getStatusCommand(), Duration.ofSeconds(15));
            return result.exitCode() == 0 && !result.output().isBlank()
                    ? new Check(ApplicationView.Status.RUNNING, result.output().trim())
                    : new Check(ApplicationView.Status.STOPPED, result.output().trim());
        } catch (RuntimeException e) {
            return new Check(ApplicationView.Status.UNAVAILABLE, e.getMessage());
        }
    }

    private void executeLifecycle(ApplicationDefinition app, Operation operation) {
        if (!app.isLifecycleEnabled()) throw new IllegalStateException("Lifecycle control is disabled: " + app.getId());
        List<String> command = operation == Operation.START ? app.getStartCommand() : app.getStopCommand();
        Result result = execute(app, command, operation == Operation.START ? Duration.ofMinutes(15) : Duration.ofMinutes(5));
        if (result.exitCode() != 0) throw new IllegalStateException(operation + " failed: " + result.output());
    }

    private Result execute(ApplicationDefinition app, List<String> command, Duration timeout) {
        if (command == null || command.isEmpty()) throw new IllegalStateException("Command is not configured");
        try {
            Process process = new ProcessBuilder(command)
                    .directory(Path.of(app.getSourceRoot()).toFile())
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            AtomicReference<IOException> readFailure = new AtomicReference<>();
            Thread outputReader = Thread.ofVirtual().name("lifecycle-output-reader").start(() -> {
                try (var input = process.getInputStream()) {
                    input.transferTo(output);
                } catch (IOException e) {
                    readFailure.set(e);
                }
            });
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                outputReader.join(5_000);
                throw new IllegalStateException("Command timed out");
            }
            outputReader.join(5_000);
            if (outputReader.isAlive()) throw new IllegalStateException("Command output reader did not finish");
            if (readFailure.get() != null) throw new IllegalStateException("Cannot read command output", readFailure.get());
            return new Result(process.exitValue(), output.toString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot run " + command.getFirst() + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Command interrupted", e);
        }
    }

    private enum Operation { START, STOP }
    private record Check(ApplicationView.Status status, String detail) {}
    private record Result(int exitCode, String output) {}
}
