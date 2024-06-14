package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.net.URI;

public interface AuthorityResolver {
    URI resolveAuthority(URI destinationURI, Message message, HttpRequestData requestData);
}
