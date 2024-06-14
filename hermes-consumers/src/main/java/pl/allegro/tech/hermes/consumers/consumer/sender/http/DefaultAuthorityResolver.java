package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.net.URI;

public class DefaultAuthorityResolver implements AuthorityResolver {
    @Override
    public URI resolveAuthority(URI destinationURI, Message message, HttpRequestData requestData) {
        return destinationURI;
    }
}
