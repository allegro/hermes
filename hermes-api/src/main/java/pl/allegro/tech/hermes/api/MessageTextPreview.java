package pl.allegro.tech.hermes.api;

public class MessageTextPreview {

    private final String content;

    private final boolean truncated;

    public MessageTextPreview(String content, boolean truncated) {
        this.content = content;
        this.truncated = truncated;
    }

    public String getContent() {
        return content;
    }

    public boolean isTruncated() {
        return truncated;
    }
}
