package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.schema.SchemaId;

public class SchemaAwarePayload {
  private final byte[] payload;
  private final SchemaId schemaId;

  public SchemaAwarePayload(byte[] payload, SchemaId schemaId) {
    this.payload = payload;
    this.schemaId = schemaId;
  }

  public byte[] getPayload() {
    return payload;
  }

  public SchemaId getSchemaId() {
    return schemaId;
  }
}
