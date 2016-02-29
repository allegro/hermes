package pl.allegro.tech.hermes.domain.topic.schema;

import org.apache.avro.Schema;

public class AvroSchemas {

    private final Schema schema;
    private final Schema schemaWithoutMetadata;
    private final int version;

    public AvroSchemas(Schema schema, Schema schemaWithoutMetadata, int version) {
        this.schema = schema;
        this.schemaWithoutMetadata = schemaWithoutMetadata;
        this.version = version;
    }

    public Schema getSchema() {
        return schema;
    }

    public Schema getSchemaWithoutMetadata() {
        return schemaWithoutMetadata;
    }

    public int getVersion() {
        return version;
    }
}
