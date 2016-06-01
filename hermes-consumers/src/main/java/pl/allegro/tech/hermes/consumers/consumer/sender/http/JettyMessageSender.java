package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;

import java.util.concurrent.CompletableFuture;

public class JettyMessageSender extends CompletableFutureAwareMessageSender {

    private final HttpRequestFactory requestFactory;
    private final ResolvableEndpointAddress endpoint;

    public JettyMessageSender(HttpRequestFactory requestFactory, ResolvableEndpointAddress endpoint) {
        this.requestFactory = requestFactory;
        this.endpoint = endpoint;
    }

    @Override
    protected void sendMessage(Message message, final CompletableFuture<MessageSendingResult> resultFuture) {
        try {
            requestFactory.buildRequest(message, endpoint.resolveFor(message))
                .send(result -> resultFuture.complete(MessageSendingResult.of(result)));
        } catch (EndpointAddressResolutionException exception) {
            resultFuture.complete(MessageSendingResult.failedResult(exception));
        }
    }

    @Override
    public void stop() {
    }
}
