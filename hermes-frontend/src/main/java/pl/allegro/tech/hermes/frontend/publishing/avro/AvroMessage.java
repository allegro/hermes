package pl.allegro.tech.hermes.frontend.publishing.avro;

import com.google.common.collect.ImmutableMap;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.util.Map;
import java.util.Optional;

public class AvroMessage implements Message {

    private final String id;
    private final byte[] data;
    private final long timestamp;
    private final CompiledSchema<Schema> schema;
    private final String partitionKey;
    private final Map<String, String> extraRequestHeaders;

    public AvroMessage(String id,
                       byte[] data,
                       long timestamp,
                       CompiledSchema<Schema> schema,
                       String partitionKey,
                       Map<String, String> extraRequestHeaders) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
        this.schema = schema;
        this.partitionKey = partitionKey;
        this.extraRequestHeaders = ImmutableMap.copyOf(extraRequestHeaders);
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
        return ContentType.AVRO;
    }

    @Override
    public String getPartitionKey() {
        return partitionKey;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<CompiledSchema<T>> getCompiledSchema() {
        return Optional.of((CompiledSchema<T>) schema);
    }

    @Override
    public Map<String, String> getExtraRequestHeaders() {
        return extraRequestHeaders;
    }

    public AvroMessage withDataReplaced(byte[] newData) {
        return new AvroMessage(id, newData, timestamp, schema, partitionKey, extraRequestHeaders);
    }
}
