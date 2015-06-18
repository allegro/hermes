package pl.allegro.tech.hermes.tracker.elasticsearch;

public enum TypedIndex implements LogSchemaAware {

    PUBLISHED_MESSAGES(PUBLISHED_INDEX, PUBLISHED_TYPE), SENT_MESSAGES(SENT_INDEX, SENT_TYPE);

    private final String index;
    private final String type;

    TypedIndex(String index, String type) {
        this.index = index;
        this.type = type;
    }

    public String getIndex() {
        return index;
    }

    public String getType() {
        return type;
    }
}
