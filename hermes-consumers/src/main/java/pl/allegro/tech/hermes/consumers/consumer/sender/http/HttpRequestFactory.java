package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.auth.HttpAuthorizationProvider;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.RETRY_COUNT;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SCHEMA_VERSION;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SUBSCRIPTION_NAME;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.TOPIC_NAME;

class HttpRequestFactory {

    private final HttpClient client;
    private final long timeout;
    private final long socketTimeout;
    private final MetadataAppender<Request> metadataAppender;
    private final Optional<HttpAuthorizationProvider>  authorizationProvider;
    private final boolean http2Enabled;

    HttpRequestFactory(HttpClient client, long timeout, long socketTimeout,
                       MetadataAppender<Request> metadataAppender,
                       Optional<HttpAuthorizationProvider> authorizationProvider, boolean http2Enabled) {
        this.client = client;
        this.timeout = timeout;
        this.socketTimeout = socketTimeout;
        this.metadataAppender = metadataAppender;
        this.authorizationProvider = authorizationProvider;
        this.http2Enabled = http2Enabled;
    }

    private final Function<ContentType, String> contentTypeToMediaType = contentType ->
            AVRO.equals(contentType) ? AVRO_BINARY : APPLICATION_JSON;

    Request buildRequest(Message message, URI uri) {
        Request request = client.newRequest(uri)
                .method(HttpMethod.POST)
                .header(MESSAGE_ID.getName(), message.getId())
                .header(RETRY_COUNT.getName(), Integer.toString(message.getRetryCounter()))
                .header(HttpHeader.CONTENT_TYPE.toString(), contentTypeToMediaType.apply(message.getContentType()))
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .idleTimeout(socketTimeout, TimeUnit.MILLISECONDS)
                .content(new BytesContentProvider(message.getData()));

        if (!http2Enabled) {
            request.header(HttpHeader.KEEP_ALIVE.toString(), "true");
        }

        if (message.hasSubscriptionIdentityHeaders()) {
            request.header(TOPIC_NAME.getName(), message.getTopic());
            request.header(SUBSCRIPTION_NAME.getName(), message.getSubscription());
        }

        message.getSchema().ifPresent(schema -> request.header(SCHEMA_VERSION.getName(), valueOf(schema.getVersion().value())));
        authorizationProvider.ifPresent(p -> p.authorizationToken()
                .ifPresent(token -> request.header(HttpHeader.AUTHORIZATION.toString(), token)));

        metadataAppender.append(request, message);

        message.getAdditionalHeaders().forEach(header -> request.header(header.getName(), header.getValue()));

        return request;
    }
}
