package pl.allegro.tech.hermes.schema;

public abstract class SubjectNamingStrategyRawSchemaClient implements RawSchemaClient {

    protected boolean suffixedSubjectNamingStrategy;

    public SubjectNamingStrategyRawSchemaClient(boolean suffixedSubjectNamingStrategy) {
        this.suffixedSubjectNamingStrategy = suffixedSubjectNamingStrategy;
    }

    protected String prepareSubjectName(String topicName) {
        if (suffixedSubjectNamingStrategy) return topicName + "-value";
        else return topicName;
    }
}
