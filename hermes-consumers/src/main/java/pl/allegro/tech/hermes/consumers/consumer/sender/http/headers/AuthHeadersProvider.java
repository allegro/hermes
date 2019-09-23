package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider;

import java.util.Optional;

public final class AuthHeadersProvider implements HttpHeadersProvider {

    private final HttpHeadersProvider headersProvider;
    private final HttpAuthorizationProvider authorizationProvider;

    public AuthHeadersProvider(HttpHeadersProvider headersProvider, HttpAuthorizationProvider authorizationProvider) {
        this.headersProvider = headersProvider;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public HttpRequestHeaders getHeaders(Message message) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        if (headersProvider != null) {
            builder.putAll(headersProvider.getHeaders(message).asMap());
        }

        if (authorizationProvider != null) {
            Optional<String> token = authorizationProvider.authorizationToken();
            token.ifPresent(s -> builder.put(HttpHeader.AUTHORIZATION.toString(), s));
        }

        return new HttpRequestHeaders(builder.build());
    }

}
