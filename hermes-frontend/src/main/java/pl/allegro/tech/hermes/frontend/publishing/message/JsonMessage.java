package pl.allegro.tech.hermes.frontend.publishing.message;

import pl.allegro.tech.hermes.api.ContentType;

public class JsonMessage implements Message {

    private final String id;
    private final byte[] data;
    private final long timestamp;

    public JsonMessage(String id, byte[] data, long timestamp) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
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

    public JsonMessage withDataReplaced(byte[] newData) {
        return new JsonMessage(id, newData, timestamp);
    }

}
