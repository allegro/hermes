package pl.allegro.tech.hermes.common.http;

public enum MessageMetadataHeaders {

    MESSAGE_ID("Hermes-Message-Id"),
    TOPIC_NAME("Hermes-Topic-Name"),
    TRACE_ID("Trace-Id");

    private final String headerName;

    MessageMetadataHeaders(String headerName) {
        this.headerName = headerName;
    }

    public String getName() {
        return this.headerName;
    }

    public String getCamelCaseName() {
        return this.headerName.replace("-", "");
    }
}
