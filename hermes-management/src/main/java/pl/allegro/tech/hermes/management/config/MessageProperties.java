package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import pl.allegro.tech.hermes.common.config.Configs;

@ConfigurationProperties(prefix = "message")
public class MessageProperties {

    private String contentRoot = Configs.MESSAGE_CONTENT_ROOT.getDefaultValue();

    private String metadataContentRoot = Configs.METADATA_CONTENT_ROOT.getDefaultValue();

    private boolean schemaIdHeaderEnabled = Configs.SCHEMA_ID_HEADER_ENABLED.getDefaultValue();

    private boolean schemaVersionTruncationEnabled = Configs.SCHEMA_VERSION_TRUNCATION_ENABLED.getDefaultValue();

    public String getContentRoot() {
        return contentRoot;
    }

    public void setContentRoot(String contentRoot) {
        this.contentRoot = contentRoot;
    }

    public String getMetadataContentRoot() {
        return metadataContentRoot;
    }

    public void setMetadataContentRoot(String metadataContentRoot) {
        this.metadataContentRoot = metadataContentRoot;
    }

    public boolean isSchemaIdHeaderEnabled() { return schemaIdHeaderEnabled; }

    public void setSchemaIdHeaderEnabled(boolean schemaIdHeaderEnabled) { this.schemaIdHeaderEnabled = schemaIdHeaderEnabled; }

    public boolean isSchemaVersionTruncationEnabled() {
        return schemaVersionTruncationEnabled;
    }

    public void setSchemaVersionTruncationEnabled(boolean schemaVersionTruncationEnabled) {
        this.schemaVersionTruncationEnabled = schemaVersionTruncationEnabled;
    }
}
