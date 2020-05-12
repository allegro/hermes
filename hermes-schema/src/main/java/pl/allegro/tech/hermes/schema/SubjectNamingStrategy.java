package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.TopicName;

public interface SubjectNamingStrategy {

    String apply(TopicName topic);

    SubjectNamingStrategy qualifiedName = TopicName::qualifiedName;

    default SubjectNamingStrategy withNamespacePrefixIf(boolean enabled, String namespace) {
        return enabled ? this : subject -> namespace + "." + subject;
    }

    default SubjectNamingStrategy withValueSuffixIf(boolean valueSuffixEnabled) {
        return valueSuffixEnabled ? subject -> subject + "-value" : this;
    }
}
