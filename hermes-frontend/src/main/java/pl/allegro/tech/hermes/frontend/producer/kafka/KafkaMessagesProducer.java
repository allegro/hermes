package pl.allegro.tech.hermes.frontend.producer.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResult;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducer;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingObserver;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResults;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Singleton
public class KafkaMessagesProducer implements BrokerMessagesProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaMessagesProducer.class);

    private final BrokerMessageProducer brokerMessageProducer;

    @Inject
    public KafkaMessagesProducer(BrokerMessageProducer brokerMessageProducer) {
        this.brokerMessageProducer = brokerMessageProducer;
    }

    @Override
    public BrokerMessagesProducingResults publishMessages(@NotNull CachedTopic topic, List<Message> messages, long timeoutMs) {
        if (messages.size() > 0) {
            final BrokerMessagesProducingObserver observer = new KafkaMessagesProducingObserver(messages.size(), timeoutMs);
            messages.forEach(it -> publishMessage(topic, it, observer));
            BrokerMessagesProducingResults validationResults = observer.waitForMessagesProducingResults();
            logger.info("PublishingStartupValidationResults:{}", validationResults);
            return validationResults;
        } else {
            return new BrokerMessagesProducingResults(Collections.emptyList());
        }
    }

    private void publishMessage(CachedTopic topic, Message message, final BrokerMessagesProducingObserver observer) {
        brokerMessageProducer.send(message, topic, new PublishingCallback() {
            @Override
            public void onPublished(Message message, Topic topic) {
                observer.notifyAboutBrokerMessageProducingResult(BrokerMessagesProducingResult.SUCCESS);
            }

            @Override
            public void onUnpublished(Message message, Topic topic, Exception exception) {
                logger.error("Failed to publish message", exception);
                observer.notifyAboutBrokerMessageProducingResult(BrokerMessagesProducingResult.FAILURE);
            }
        });
    }
}
