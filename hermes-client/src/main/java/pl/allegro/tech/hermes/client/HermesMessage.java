package pl.allegro.tech.hermes.client;

import java.nio.charset.Charset;

/**
 * All information Hermes needs to send a message.
 */
public class HermesMessage {

    private static final Charset CHARSET = Charset.forName("UTF-8");

    private final String topic;
    private final String contentType;
    private final int schemaVersion;
    private final byte[] body;

    public HermesMessage(String topic, String contentType, int schemaVersion, byte[] body) {
        this.topic = topic;
        this.contentType = contentType;
        this.schemaVersion = schemaVersion;
        this.body = body;
    }

    public HermesMessage(String topic, String contentType, byte[] body) {
        this(topic, contentType, -1, body);
    }

    /**
     * Message on given topic with given MIME Content Type.
     *
     * @param topic topic name
     * @param contentType MIME content type
     * @param body body which will be translated to byte[] using UTF-8 charset
     */
    public HermesMessage(String topic, String contentType, String body) {
        this(topic, contentType, body.getBytes(CHARSET));
    }

    @Deprecated
    public HermesMessage(String topic, String body) {
        this(topic, null, body);
    }

    static HermesMessage appendContentType(HermesMessage message, String contentType) {
        return new HermesMessage(message.getTopic(), contentType, message.getBody());
    }

    public String getTopic() {
        return topic;
    }

    public byte[] getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public boolean schemaVersionDefined() {
        return schemaVersion >= 0;
    }

    @Override
    public String toString() {
        return new String(getBody(), CHARSET);
    }
}
