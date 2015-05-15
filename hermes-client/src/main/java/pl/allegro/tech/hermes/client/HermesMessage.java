package pl.allegro.tech.hermes.client;

public class HermesMessage {
    private final String topic;
    private final String body;

    public HermesMessage(String topic, String body) {
        this.topic = topic;
        this.body = body;
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

    @Override
    public String toString() {
        return getBody();
    }
}
