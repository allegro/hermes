package pl.allegro.tech.hermes.frontend.producer.kafka;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class FallbackToRemoteDatacenterAwareMessageProducer implements BrokerMessageProducer {

  private final BrokerMessageProducer localDatacenterMessageProducer;
  private final BrokerMessageProducer multiDatacenterMessageProducer;

  public FallbackToRemoteDatacenterAwareMessageProducer(
      BrokerMessageProducer localDatacenterMessageProducer,
      BrokerMessageProducer multiDatacenterMessageProducer) {
    this.localDatacenterMessageProducer = localDatacenterMessageProducer;
    this.multiDatacenterMessageProducer = multiDatacenterMessageProducer;
  }

  @Override
  public void send(Message message, CachedTopic topic, PublishingCallback callback) {
    if (topic.getTopic().isFallbackToRemoteDatacenterEnabled()) {
      this.multiDatacenterMessageProducer.send(message, topic, callback);
    } else {
      this.localDatacenterMessageProducer.send(message, topic, callback);
    }
  }

  @Override
  public boolean areAllTopicsAvailable() {
    return localDatacenterMessageProducer.areAllTopicsAvailable();
  }

  @Override
  public boolean isTopicAvailable(CachedTopic topic) {
    return localDatacenterMessageProducer.isTopicAvailable(topic);
  }
}
