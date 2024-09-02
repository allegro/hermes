package pl.allegro.tech.hermes.benchmark.environment;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

public class InMemoryBrokerMessageProducer implements BrokerMessageProducer {

  @Override
  public void send(Message message, CachedTopic topic, PublishingCallback callback) {
    callback.onPublished(message, topic.getTopic());
  }

  @Override
  public boolean areAllTopicsAvailable() {
    return true;
  }

  @Override
  public boolean isTopicAvailable(CachedTopic cachedTopic) {
    return true;
  }
}
