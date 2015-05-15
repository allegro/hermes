package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.consumers.consumer.message.RawMessage;

public interface MessageReceiver {

    RawMessage next();
    void stop();

}
