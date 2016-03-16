package pl.allegro.tech.hermes.common.http;

public enum MessageMetadataHeaders {

    MESSAGE_ID("Hermes-Message-Id"),
    BATCH_ID("Hermes-Batch-Id"),
    TOPIC_NAME("Hermes-Topic-Name"),
    RETRY_COUNT("Hermes-Retry-Count"),
    SCHEMA_VERSION("Hermes-Schema-Version");

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
