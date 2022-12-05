package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import com.google.common.collect.ImmutableList;
import org.eclipse.jetty.client.api.Request;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.ResilientMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.MultiMessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.SingleMessageSendingResult;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpRequestData.HttpRequestDataBuilder;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.HttpRequestHeaders;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.EndpointAddressResolutionException;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.ResolvableEndpointAddress;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JettyBroadCastMessageSender implements MessageSender {

    private final HttpRequestFactory requestFactory;
    private final ResolvableEndpointAddress endpoint;
    private final HttpHeadersProvider requestHeadersProvider;
    private final SendingResultHandlers sendingResultHandlers;
    private final Function<Throwable, SingleMessageSendingResult> exceptionMapper = MessageSendingResult::failedResult;
    private final ResilientMessageSender resilientMessageSender;

    public JettyBroadCastMessageSender(HttpRequestFactory requestFactory,
                                       ResolvableEndpointAddress endpoint,
                                       HttpHeadersProvider requestHeadersProvider,
                                       SendingResultHandlers sendingResultHandlers,
                                       ResilientMessageSender resilientMessageSender
    ) {
        this.requestFactory = requestFactory;
        this.endpoint = endpoint;
        this.requestHeadersProvider = requestHeadersProvider;
        this.sendingResultHandlers = sendingResultHandlers;
        this.resilientMessageSender = resilientMessageSender;
    }

    @Override
    public CompletableFuture<MessageSendingResult> send(Message message) {
        try {
            return sendMessage(message).thenApply(MultiMessageSendingResult::new);
        } catch (Exception exception) {
            return CompletableFuture.completedFuture(exceptionMapper.apply(exception));
        }
    }

    private CompletableFuture<List<SingleMessageSendingResult>> sendMessage(Message message) {
        try {
            List<CompletableFuture<SingleMessageSendingResult>> results = collectResults(message);
            return mergeResults(results);
        } catch (EndpointAddressResolutionException exception) {
            return CompletableFuture.completedFuture(Collections.singletonList(exceptionMapper.apply(exception)));
        }
    }

    private List<CompletableFuture<SingleMessageSendingResult>> collectResults(
            Message message
    ) throws EndpointAddressResolutionException {

        final HttpRequestData requestData = new HttpRequestDataBuilder()
                .withRawAddress(endpoint.getRawAddress())
                .build();

        HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message, requestData);

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
        return resilientMessageSender.send(
                resultFuture -> request.send(sendingResultHandlers.handleSendingResultForBroadcast(resultFuture)),
                exceptionMapper);

    }

    @Override
    public void stop() {
    }

}
