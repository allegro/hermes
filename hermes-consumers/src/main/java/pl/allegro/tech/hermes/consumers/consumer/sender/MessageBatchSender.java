package pl.allegro.tech.hermes.consumers.consumer.sender;

import pl.allegro.tech.hermes.api.EndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;

public interface MessageBatchSender {
    MessageSendingResult send(MessageBatch message, EndpointAddress address, int requestTimeout);
}
