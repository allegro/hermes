package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.consumers.consumer.Message;

public interface HttpAuthorizationProvider {

    String authorizationToken(Message message);

}
