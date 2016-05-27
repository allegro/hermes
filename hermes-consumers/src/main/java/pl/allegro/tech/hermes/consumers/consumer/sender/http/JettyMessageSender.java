package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.RETRY_COUNT;
import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.SCHEMA_VERSION;
import static pl.allegro.tech.hermes.consumers.consumer.sender.http.AvroMediaType.AVRO_BINARY;

public class JettyMessageSender extends CompletableFutureAwareMessageSender {

    private final HttpClient client;
    private final ResolvableEndpointAddress endpoint;
    private final Optional<HttpAuthorizationProvider> authorizationProvider;
    private final long timeout;
    private final MetadataAppender<Request> metadataAppender;

    public JettyMessageSender(HttpClient client,
                              ResolvableEndpointAddress endpoint,
                              Optional<HttpAuthorizationProvider> authorizationProvider,
                              int timeout,
                              MetadataAppender<Request> metadataAppender) {
        this.client = client;
        this.endpoint = endpoint;
        this.authorizationProvider = authorizationProvider;
        this.timeout = timeout;
        this.metadataAppender = metadataAppender;
    }

    @Override
    protected void sendMessage(Message message, final CompletableFuture<MessageSendingResult> resultFuture) {
        try {
            buildRequest(message)
                .send(result -> resultFuture.complete(new MessageSendingResult(result)));
        } catch (EndpointAddressResolutionException exception) {
            resultFuture.complete(MessageSendingResult.failedResult(exception));
        }
    }

    private Request buildRequest(Message message) throws EndpointAddressResolutionException {
        Request request = client.newRequest(endpoint.resolveFor(message))
                .method(HttpMethod.POST)
                .header(HttpHeader.KEEP_ALIVE.toString(), "true")
                .header(MESSAGE_ID.getName(), message.getId())
                .header(RETRY_COUNT.getName(), Integer.toString(message.getRetryCounter()))
                .header(HttpHeader.CONTENT_TYPE.toString(), messageContentType(message))
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .content(new BytesContentProvider(message.getData()));

        authorizationProvider.ifPresent(p -> request.header(HttpHeader.AUTHORIZATION.toString(), p.authorizationToken(message)));
        message.getSchema().ifPresent(schema -> request.header(SCHEMA_VERSION.getName(), valueOf(schema.getVersion().value())));

        metadataAppender.append(request, message);
        message.getAdditionalHeaders().entrySet()
                .forEach(header -> request.header(header.getKey(), header.getValue()));

        return request;
    }

    private String messageContentType(Message message) {
        return AVRO.equals(message.getContentType()) ? AVRO_BINARY : APPLICATION_JSON;
    }

    @Override
    public void stop() {
    }

}
