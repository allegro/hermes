package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

interface TopicMetadataLoader {

  MetadataLoadingResult load(CachedTopic cachedTopic);

  record MetadataLoadingResult(Type type, TopicName topicName, String datacenter) {

    static MetadataLoadingResult success(TopicName topicName, String datacenter) {
      return new MetadataLoadingResult(Type.SUCCESS, topicName, datacenter);
    }

    static MetadataLoadingResult failure(TopicName topicName, String datacenter) {
      return new MetadataLoadingResult(Type.FAILURE, topicName, datacenter);
    }

    boolean isFailure() {
      return Type.FAILURE == type;
    }
  }

  enum Type {
    SUCCESS,
    FAILURE
  }
}
