package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.api.Subscription;

public interface ReceiverFactory {

    MessageReceiver createMessageReceiver(Subscription subscription);

}
