package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "audit")
public class AuditProperties {

    private boolean auditManagementOperations = false;

    public boolean isAuditManagementOperations() {
        return auditManagementOperations;
    }

    public void setAuditManagementOperations(boolean auditManagementOperations) {
        this.auditManagementOperations = auditManagementOperations;
    }
}
