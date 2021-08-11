package pl.allegro.tech.hermes.frontend.producer.kafka

import groovy.transform.builder.Builder
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback
import pl.allegro.tech.hermes.frontend.publishing.message.Message

@Builder
class TestBrokerMessageProducer implements BrokerMessageProducer {

    boolean throwException = false
    Collection<Message> failedPublishedMessages = Collections.emptyList()

    @Override
    void send(Message message, CachedTopic topic, PublishingCallback callback) {
        if (!throwException) {
            if (failedPublishedMessages.any { it == message }) {
                callback.onUnpublished(message, topic.topic, new RuntimeException("Test error message"))
            } else {
                callback.onPublished(message, topic.topic)
            }
        } else {
            throw new RuntimeException("Test error message")
        }
    }

    @Override
    boolean isTopicAvailable(CachedTopic topic) {
        return true
    }
}
