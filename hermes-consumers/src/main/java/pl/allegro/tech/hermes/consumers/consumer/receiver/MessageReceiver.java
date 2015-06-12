package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface MessageReceiver {

    Message next();
    void stop();

}
