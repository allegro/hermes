package pl.allegro.tech.hermes.common.http;

public enum MessageMetadataHeaders {

    MESSAGE_ID("Hermes-Message-Id"),
    TOPIC_NAME("Hermes-Topic-Name"),
    TRACE_ID("Trace-Id"),
    SPAN_ID("Span-Id"),
    PARENT_SPAN_ID("Parent-Span-Id"),
    TRACE_SAMPLED("Trace-Sampled"),
    TRACE_REPORTED("Trace-Reported");

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
