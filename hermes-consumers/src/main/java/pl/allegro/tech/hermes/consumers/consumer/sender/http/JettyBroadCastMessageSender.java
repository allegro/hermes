package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import com.google.common.collect.ImmutableList;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.MultiMessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class JettyBroadCastMessageSender implements MessageSender {

    private final HttpRequestFactory requestFactory;
    private final ResolvableEndpointAddress endpoint;
    private final HttpHeadersProvider requestHeadersProvider;

    public JettyBroadCastMessageSender(HttpRequestFactory requestFactory,
                                       ResolvableEndpointAddress endpoint,
                                       HttpHeadersProvider requestHeadersProvider) {
        this.requestFactory = requestFactory;
        this.endpoint = endpoint;
        this.requestHeadersProvider = requestHeadersProvider;
    }

    @Override
    public CompletableFuture<MessageSendingResult> send(Message message) {
        try {
            return sendMessage(message).thenApply(MultiMessageSendingResult::new);
        } catch (Exception exception) {
            return CompletableFuture.completedFuture(MessageSendingResult.failedResult(exception));
        }
    }

    private CompletableFuture<List<SingleMessageSendingResult>> sendMessage(Message message) {
        try {
            List<CompletableFuture<SingleMessageSendingResult>> results = collectResults(message);
            return mergeResults(results);
        } catch (EndpointAddressResolutionException exception) {
            return CompletableFuture.completedFuture(Collections.singletonList(MessageSendingResult.failedResult(exception)));
        }
    }

    private List<CompletableFuture<SingleMessageSendingResult>> collectResults(Message message) throws EndpointAddressResolutionException {
        String rawAddress = endpoint.getRawAddress();
        HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message, rawAddress);

        return endpoint.resolveAllFor(message).stream()
                .filter(uri -> message.hasNotBeenSentTo(uri.toString()))
                .map(uri -> requestFactory.buildRequest(message, uri, headers))
                .map(this::processResponse)
                .collect(Collectors.toList());
    }

    private CompletableFuture<List<SingleMessageSendingResult>> mergeResults(List<CompletableFuture<SingleMessageSendingResult>> results) {
        return CompletableFuture.allOf(results.toArray(new CompletableFuture[results.size()]))
                .thenApply(v -> results.stream()
                        .map(CompletableFuture::join)
                        .reduce(
                                ImmutableList.<SingleMessageSendingResult>builder(),
                                (builder, element) -> builder.add(element),
                                (listA, listB) -> listA.addAll(listB.build())
                        ).build());
    }

    private CompletableFuture<SingleMessageSendingResult> processResponse(Request request) {
        CompletableFuture<SingleMessageSendingResult> future = new CompletableFuture<>();
        Response.CompleteListener completeListener = result -> future.complete(MessageSendingResult.of(result));
        request.send(completeListener);
        return future;
    }

    @Override
    public void stop() {
    }

}
