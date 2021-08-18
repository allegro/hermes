package pl.allegro.tech.hermes.frontend.producer;

import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface BrokerMessagesProducer {
    BrokerMessagesBatchProducingResults publishMessages(@NotNull CachedTopic topic, List<Message> messages, long timeoutMs);
}
