package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import javax.ws.rs.core.MediaType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static pl.allegro.tech.hermes.common.http.MessageMetadataHeaders.MESSAGE_ID;

public class JettyMessageSender extends CompletableFutureAwareMessageSender {

    private final HttpClient client;
    private final ResolvableEndpointAddress endpoint;
    private final long timeout;
    private final MetadataAppender<Request> metadataAppender;

    private final Function<ContentType, String> contentTypeToMediaType = contentType -> {
        if (ContentType.AVRO.equals(contentType)) {
            return pl.allegro.tech.hermes.consumers.consumer.sender.http.MediaType.AVRO;
        }

        return MediaType.APPLICATION_JSON;
    };

    public JettyMessageSender(HttpClient client, ResolvableEndpointAddress endpoint, int timeout, MetadataAppender<Request> metadataAppender) {
        this.client = client;
        this.endpoint = endpoint;
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
                .header(HttpHeader.CONTENT_TYPE.toString(), contentTypeToMediaType.apply(message.getContentType()))
                .timeout(timeout, TimeUnit.MILLISECONDS)
                .content(new BytesContentProvider(message.getData()));
        return appendTraceInfo(request, message);
    }

    private Request appendTraceInfo(Request request, Message message) {
        return metadataAppender.append(request, message);
    }

    @Override
    public void stop() {
    }

}
