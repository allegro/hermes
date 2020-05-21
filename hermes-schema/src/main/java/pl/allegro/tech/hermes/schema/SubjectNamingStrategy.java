package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.TopicName;

public interface SubjectNamingStrategy {

    String apply(TopicName topic);

    SubjectNamingStrategy qualifiedName = TopicName::qualifiedName;

    default SubjectNamingStrategy withNamespacePrefixIf(boolean enabled, String namespace) {
        return enabled ? topicName -> namespace + "." + this.apply(topicName) : this;
    }

    default SubjectNamingStrategy withValueSuffixIf(boolean valueSuffixEnabled) {
        return valueSuffixEnabled ? topicName -> this.apply(topicName) + "-value" : this;
    }
}
