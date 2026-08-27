package com.platform.service;

import com.platform.domain.ApplicationDefinition;
import com.platform.registry.ApplicationRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationCatalogPersistence {
    private final JdbcTemplate jdbc;
    private final ApplicationRegistry registry;

    public ApplicationCatalogPersistence(JdbcTemplate jdbc, ApplicationRegistry registry) {
        this.jdbc = jdbc;
        this.registry = registry;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void synchronize() {
        List<ApplicationDefinition> applications = registry.findAll();
        for (ApplicationDefinition app : applications) upsert(app);
        if (applications.isEmpty()) {
            jdbc.update("DELETE FROM platform_application");
        } else {
            String placeholders = String.join(",", applications.stream().map(ignored -> "?").toList());
            jdbc.update("DELETE FROM platform_application WHERE id NOT IN (" + placeholders + ")",
                    applications.stream().map(ApplicationDefinition::getId).toArray());
        }
    }

    private void upsert(ApplicationDefinition app) {
        int updated = jdbc.update("""
                UPDATE platform_application SET name=?, description=?, category=?, source_root=?,
                  runtime_type=?, lifecycle_enabled=?, sort_order=?, updated_at=? WHERE id=?
                """, app.getName(), app.getDescription(), app.getCategory(), app.getSourceRoot(),
                app.getRuntime().name(), app.isLifecycleEnabled(), app.getSortOrder(), Timestamp.valueOf(LocalDateTime.now()), app.getId());
        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO platform_application
                      (id,name,description,category,source_root,runtime_type,lifecycle_enabled,sort_order,updated_at)
                    VALUES (?,?,?,?,?,?,?,?,?)
                    """, app.getId(), app.getName(), app.getDescription(), app.getCategory(), app.getSourceRoot(),
                    app.getRuntime().name(), app.isLifecycleEnabled(), app.getSortOrder(), Timestamp.valueOf(LocalDateTime.now()));
        }
        jdbc.update("DELETE FROM platform_component WHERE application_id=?", app.getId());
        int componentOrder = 0;
        for (ApplicationDefinition.Component component : app.getComponents()) {
            jdbc.update("""
                    INSERT INTO platform_component
                      (id,application_id,name,component_kind,source_path,technology,sort_order)
                    VALUES (?,?,?,?,?,?,?)
                    """, component.id(), app.getId(), component.name(), component.kind().name(),
                    component.sourcePath(), component.technology(), componentOrder++);
        }
        jdbc.update("DELETE FROM platform_application_link WHERE application_id=?", app.getId());
        int linkOrder = 0;
        for (ApplicationDefinition.Link link : app.getLinks()) {
            jdbc.update("INSERT INTO platform_application_link (application_id,name,url,sort_order) VALUES (?,?,?,?)",
                    app.getId(), link.name(), link.url(), linkOrder++);
        }
    }
}
