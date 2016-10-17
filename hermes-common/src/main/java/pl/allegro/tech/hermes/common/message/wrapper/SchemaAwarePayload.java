package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.schema.SchemaVersion;

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