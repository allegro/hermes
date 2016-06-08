package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatchReceiver;

public interface ReceiverFactory {

    MessageReceiver createMessageReceiver(Topic receivingTopic, Subscription subscription);

}
