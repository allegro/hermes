package pl.allegro.tech.hermes.frontend.publishing.message;

public class Message {

    private final String id;
    private final String traceId;
    private final byte[] data;
    private final long timestamp;

    public Message(String id, String traceId, byte[] data, long timestamp) {
        this.id = id;
        this.traceId = traceId;
        this.data = data;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getTraceId() {
        return traceId;
    }

    public byte[] getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Message withDataReplaced(byte[] newData) {
        return new Message(id, traceId, newData, timestamp);
    }

}
