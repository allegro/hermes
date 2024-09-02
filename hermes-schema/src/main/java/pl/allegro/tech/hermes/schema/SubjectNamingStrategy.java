package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.TopicName;

public interface SubjectNamingStrategy {

  class Namespace {
    private final String value;
    private final String separator;

    public Namespace(String value, String separator) {
      this.value = value;
      this.separator = separator;
    }

    public String getValue() {
      return value;
    }

    public String apply(String name) {
      return value.isEmpty() ? name : value + separator + name;
    }
  }

  String apply(TopicName topic);

  SubjectNamingStrategy qualifiedName = TopicName::qualifiedName;

  default SubjectNamingStrategy withNamespacePrefixIf(boolean enabled, Namespace namespace) {
    return enabled ? topicName -> namespace.apply(this.apply(topicName)) : this;
  }

  default SubjectNamingStrategy withValueSuffixIf(boolean valueSuffixEnabled) {
    return valueSuffixEnabled ? topicName -> this.apply(topicName) + "-value" : this;
  }
}
