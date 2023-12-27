package pl.allegro.tech.hermes.frontend.publishing.message;

import pl.allegro.tech.hermes.api.ContentType;

import java.util.Map;

public class JsonMessage implements Message {

    private final String id;
    private final byte[] data;
    private final long timestamp;
    private final String partitionKey;
    private final Map<String, String> propagatedHTTPHeaders;

    public JsonMessage(String id, byte[] data, long timestamp, String partitionKey, Map<String, String> propagatedHTTPHeaders) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
        this.partitionKey = partitionKey;
        this.propagatedHTTPHeaders = propagatedHTTPHeaders;
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
    public Map<String, String> getHTTPHeaders() {
        return propagatedHTTPHeaders;
    }

    public JsonMessage withDataReplaced(byte[] newData) {
        return new JsonMessage(id, newData, timestamp, partitionKey, propagatedHTTPHeaders);
    }

}
