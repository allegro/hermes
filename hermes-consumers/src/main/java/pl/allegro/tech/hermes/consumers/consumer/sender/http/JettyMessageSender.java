package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import org.eclipse.jetty.client.api.Request;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class JettyMessageSender extends CompletableFutureAwareMessageSender {

    private final HttpRequestFactory requestFactory;
    private final ResolvableEndpointAddress addressResolver;
    private final HttpHeadersProvider requestHeadersProvider;

    public JettyMessageSender(HttpRequestFactory requestFactory,
                              ResolvableEndpointAddress addressResolver,
                              HttpHeadersProvider headersProvider) {
        this.requestFactory = requestFactory;
        this.addressResolver = addressResolver;
        this.requestHeadersProvider = headersProvider;
    }

    @Override
    protected void sendMessage(Message message, final CompletableFuture<MessageSendingResult> resultFuture) {
        try {
            String rawAddress = addressResolver.getRawAddress();
            HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message, rawAddress);

            URI resolvedUri = addressResolver.resolveFor(message);
            Request request = requestFactory.buildRequest(message, resolvedUri, headers);

            request.send(result -> resultFuture.complete(MessageSendingResult.of(result)));
        } catch (EndpointAddressResolutionException exception) {
            resultFuture.complete(MessageSendingResult.failedResult(exception));
        }
    }

    @Override
    public void stop() {
    }
}
