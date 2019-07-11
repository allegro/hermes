package pl.allegro.tech.hermes.consumers.consumer.sender.http.headers;

import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider;

import java.util.Optional;
import java.util.function.Function;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;

public final class Http1RequestHeadersProvider implements HttpRequestHeadersProvider {

    private static final Function<ContentType, String> contentTypeToMediaType = contentType ->
            AVRO.equals(contentType) ? AVRO_BINARY : APPLICATION_JSON;

    private final Optional<HttpAuthorizationProvider> authorizationProvider;

    public Http1RequestHeadersProvider(Optional<HttpAuthorizationProvider> authorizationProvider) {
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public HttpRequestHeaders getHeaders(Message message) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        builder
                .put(HttpHeader.CONTENT_TYPE.toString(), contentTypeToMediaType.apply(message.getContentType()))
                .put(HttpHeader.KEEP_ALIVE.toString(), "true");

        authorizationProvider.ifPresent(p -> p.authorizationToken()
                .ifPresent(token -> builder.put(HttpHeader.AUTHORIZATION.toString(), token)));

        message.getAdditionalHeaders().forEach(header -> builder.put(header.getName(), header.getValue()));

        builder.putAll(HermesHeadersProvider.INSTANCE.getHeaders(message).asMap());

        return new HttpRequestHeaders(builder.build());
    }

}
