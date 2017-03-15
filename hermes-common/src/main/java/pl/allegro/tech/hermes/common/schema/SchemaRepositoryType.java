package pl.allegro.tech.hermes.common.schema;

public enum SchemaRepositoryType {
    SCHEMA_REPO("schema-repo"), SCHEMA_REGISTRY("schema-registry");

    private final String metricName;

    SchemaRepositoryType(String metricName) {
        this.metricName = metricName;
    }

    @Override
    public String toString() {
        return metricName;
    }
}
