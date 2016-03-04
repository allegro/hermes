package pl.allegro.tech.hermes.common.message.serialization;

import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;

public class SchemaAwarePayload {
    private final byte[] payload;
    private final SchemaVersion schemaVersion;

    public SchemaAwarePayload(byte[] payload, SchemaVersion schemaVersion) {
        this.payload = payload;
        this.schemaVersion = schemaVersion;
    }

    public byte[] getPayload() {
        return payload;
    }

    public SchemaVersion getSchemaVersion() {
        return schemaVersion;
    }
}