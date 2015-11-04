package pl.allegro.tech.hermes.frontend.publishing.message;

import pl.allegro.tech.hermes.api.TraceInfo;

public class Message {

    private final String id;
    private final TraceInfo traceInfo;
    private final byte[] data;
    private final long timestamp;

    public Message(String id, TraceInfo traceInfo, byte[] data, long timestamp) {
        this.id = id;
        this.traceInfo = traceInfo;
        this.data = data;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public TraceInfo getTraceInfo() {
        return traceInfo;
    }

    public byte[] getData() {
        return data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Message withDataReplaced(byte[] newData) {
        return new Message(id, traceInfo, newData, timestamp);
    }

}
