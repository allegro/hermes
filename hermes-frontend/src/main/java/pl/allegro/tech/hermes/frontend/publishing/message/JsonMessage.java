package pl.allegro.tech.hermes.frontend.publishing.message;

import com.google.common.collect.ImmutableMap;
import pl.allegro.tech.hermes.api.ContentType;

import java.util.Map;

public class JsonMessage implements Message {

    private final String id;
    private final byte[] data;
    private final long timestamp;
    private final String partitionKey;
    private final Map<String, String> extraRequestHeaders;

    public JsonMessage(String id, byte[] data, long timestamp, String partitionKey, Map<String, String> extraRequestHeaders) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
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
        return ContentType.JSON;
    }

    @Override
    public String getPartitionKey() {
        return partitionKey;
    }

    @Override
    public Map<String, String> getExtraRequestHeaders() {
        return extraRequestHeaders;
    }

    public JsonMessage withDataReplaced(byte[] newData) {
        return new JsonMessage(id, newData, timestamp, partitionKey, extraRequestHeaders);
    }

}
