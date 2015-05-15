package pl.allegro.tech.hermes.frontend.publishing;

public class Message {

    private final String id;
    private final byte[] data;
    private final long timestamp;

    public Message(String id, byte[] data, long timestamp) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
