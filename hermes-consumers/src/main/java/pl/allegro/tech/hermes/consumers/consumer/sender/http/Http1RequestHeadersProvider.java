package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.http.HttpHeader;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.RETRY_COUNT;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TOPIC_NAME;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SCHEMA_VERSION;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SUBSCRIPTION_NAME;

final class Http1RequestHeadersProvider implements HttpRequestHeadersProvider {

    private static final Function<ContentType, String> contentTypeToMediaType = contentType ->
            AVRO.equals(contentType) ? AVRO_BINARY : APPLICATION_JSON;

    private final Optional<HttpAuthorizationProvider> authorizationProvider;

    Http1RequestHeadersProvider(Optional<HttpAuthorizationProvider> authorizationProvider) {
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public HttpRequestHeaders getHeaders(Message message) {
        Map<String, String> headers = new HashMap<>();

        headers.put(MESSAGE_ID.getName(), message.getId());
        headers.put(RETRY_COUNT.getName(), Integer.toString(message.getRetryCounter()));
        headers.put(HttpHeader.CONTENT_TYPE.toString(), contentTypeToMediaType.apply(message.getContentType()));
        headers.put(HttpHeader.KEEP_ALIVE.toString(), "true");

        if (message.hasSubscriptionIdentityHeaders()) {
            headers.put(TOPIC_NAME.getName(), message.getTopic());
            headers.put(SUBSCRIPTION_NAME.getName(), message.getSubscription());
        }

        message.getSchema().ifPresent(schema -> headers.put(SCHEMA_VERSION.getName(), valueOf(schema.getVersion().value())));
        authorizationProvider.ifPresent(p -> p.authorizationToken()
                .ifPresent(token -> headers.put(HttpHeader.AUTHORIZATION.toString(), token)));

        message.getAdditionalHeaders().forEach(header -> headers.put(header.getName(), header.getValue()));

        return new HttpRequestHeaders(headers);
    }

}
