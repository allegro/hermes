package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.net.URI;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.RETRY_COUNT;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SCHEMA_VERSION;
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.AvroMediaType.AVRO_BINARY;

class HttpRequestFactory {

    private final HttpClient client;
    private final long timeout;
    private final MetadataAppender<Request> metadataAppender;
    private final Optional<HttpAuthorizationProvider>  authorizationProvider;

    HttpRequestFactory(HttpClient client, long timeout, MetadataAppender<Request> metadataAppender,
                       Optional<HttpAuthorizationProvider> authorizationProvider) {
        this.client = client;
        this.timeout = timeout;
        this.metadataAppender = metadataAppender;
        this.authorizationProvider = authorizationProvider;
    }

    private final Function<ContentType, String> contentTypeToMediaType = contentType ->
            AVRO.equals(contentType) ? AVRO_BINARY : APPLICATION_JSON;

    Request buildRequest(Message message, URI uri) {
        Request request = client.newRequest(uri)
                .method(HttpMethod.POST)
                .header(HttpHeader.KEEP_ALIVE.toString(), "true")
                .header(MESSAGE_ID.getName(), message.getId())
                .header(RETRY_COUNT.getName(), Integer.toString(message.getRetryCounter()))
                .header(HttpHeader.CONTENT_TYPE.toString(), contentTypeToMediaType.apply(message.getContentType()))
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .content(new BytesContentProvider(message.getData()));

        message.getSchema().ifPresent(schema -> request.header(SCHEMA_VERSION.getName(), valueOf(schema.getVersion().value())));
        authorizationProvider.ifPresent(p -> request.header(HttpHeader.AUTHORIZATION.toString(), p.authorizationToken(message)));

        metadataAppender.append(request, message);

        message.getAdditionalHeaders().forEach(header -> request.header(header.getName(), header.getValue()));

        return request;
    }
}
