package pl.allegro.tech.hermes.frontend.publishing.message;

import com.github.fge.jsonschema.main.JsonSchema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;

import java.util.Optional;

public class JsonMessage implements Message {

    private final String id;
    private final byte[] data;
    private final long timestamp;
    private final Optional<CompiledSchema<JsonSchema>> schema;

    public JsonMessage(String id, byte[] data, long timestamp) {
       this(id, data, timestamp, Optional.empty());
    }

    public JsonMessage(String id, byte[] data, long timestamp, Optional<CompiledSchema<JsonSchema>> schema) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
        this.schema = schema;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public ContentType getContentType() {
        return ContentType.JSON;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<CompiledSchema<T>> getCompiledSchema() {
        return schema.map(schema -> (CompiledSchema<T>)schema);
    }

    public JsonMessage withDataReplaced(byte[] newData) {
        return new JsonMessage(id, newData, timestamp);
    }

}
