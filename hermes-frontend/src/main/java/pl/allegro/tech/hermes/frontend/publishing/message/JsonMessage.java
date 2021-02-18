package pl.allegro.tech.hermes.frontend.publishing.message;

import pl.allegro.tech.hermes.api.ContentType;

public class JsonMessage implements Message {

    private final String id;
    private final byte[] data;
    private final long timestamp;
    private final String partitionKey;

    public JsonMessage(String id, byte[] data, long timestamp, String partitionKey) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
        this.partitionKey = partitionKey;
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

    public JsonMessage withDataReplaced(byte[] newData) {
        return new JsonMessage(id, newData, timestamp, partitionKey);
    }

}
