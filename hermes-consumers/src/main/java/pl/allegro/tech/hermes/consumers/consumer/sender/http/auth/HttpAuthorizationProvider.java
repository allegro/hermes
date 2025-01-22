package pl.allegro.tech.hermes.consumers.consumer.sender.http.auth;

import java.util.Optional;

public interface HttpAuthorizationProvider {

  Optional<String> authorizationToken();
}
