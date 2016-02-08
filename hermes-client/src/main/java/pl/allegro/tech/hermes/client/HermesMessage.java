package pl.allegro.tech.hermes.client;

public class HermesMessage {
    private final String topic;
    private final String body;
    private final String contentType;

    public HermesMessage(String topic, String body, String contentType) {
        this.topic = topic;
        this.body = body;
        this.contentType = contentType;
    }

    @Deprecated
    public HermesMessage(String topic, String body) {
        this.topic = topic;
        this.body = body;
        this.contentType = null;
    }

    static HermesMessage appendContentType(HermesMessage message, String contentType) {
        return new HermesMessage(message.getTopic(), message.getBody(), contentType);
    }

    public String getTopic() {
        return topic;
    }

    public String getSanitizedTopic() {
        int lastDot = getTopic().lastIndexOf(".");
        char[] sanitized = getTopic().replaceAll("\\.", "_").toCharArray();
        sanitized[lastDot] = '.';
        return String.valueOf(sanitized);
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public String toString() {
        return getBody();
    }
}
