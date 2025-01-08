package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.jetty.client.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

public class JettyBroadCastMessageSender implements MessageSender {

  private static final Logger logger = LoggerFactory.getLogger(JettyBroadCastMessageSender.class);

  private final HttpRequestFactory requestFactory;
  private final ResolvableEndpointAddress endpoint;
  private final HttpHeadersProvider requestHeadersProvider;
  private final SendingResultHandlers sendingResultHandlers;
  private final Function<Throwable, SingleMessageSendingResult> exceptionMapper =
      MessageSendingResult::failedResult;
  private final ResilientMessageSender resilientMessageSender;

  public JettyBroadCastMessageSender(
      HttpRequestFactory requestFactory,
      ResolvableEndpointAddress endpoint,
      HttpHeadersProvider requestHeadersProvider,
      SendingResultHandlers sendingResultHandlers,
      ResilientMessageSender resilientMessageSender) {
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
      Set<CompletableFuture<SingleMessageSendingResult>> results = collectResults(message);
      return mergeResults(results);
    } catch (EndpointAddressResolutionException exception) {
      return CompletableFuture.completedFuture(
          Collections.singletonList(exceptionMapper.apply(exception)));
    }
  }

  private Set<CompletableFuture<SingleMessageSendingResult>> collectResults(Message message)
      throws EndpointAddressResolutionException {
    var currentResults = sendPendingMessages(message);
    var results = new HashSet<>(currentResults);

    // add previously succeeded uris to the result set so that successful uris from all attempts are
    // retained.
    // this way a MessageSendingResult can be considered successful even when the last send attempt
    // did not send to any uri, e.g. because all uris returned by endpoint resolver were already
    // sent to in the past.
    for (String succeededUri : message.getSucceededUris()) {
      try {
        var uri = new URI(succeededUri);
        var result = MessageSendingResult.succeededResult(uri);
        results.add(CompletableFuture.completedFuture(result));
      } catch (URISyntaxException exception) {
        logger.error("Error while parsing already sent broadcast URI {}", succeededUri, exception);
      }
    }
    return results;
  }

  private Set<CompletableFuture<SingleMessageSendingResult>> sendPendingMessages(Message message)
      throws EndpointAddressResolutionException {
    final HttpRequestData requestData =
        new HttpRequestDataBuilder().withRawAddress(endpoint.getRawAddress()).build();

    HttpRequestHeaders headers = requestHeadersProvider.getHeaders(message, requestData);

    List<URI> resolvedUris =
        endpoint.resolveAllFor(message).stream()
            .filter(uri -> message.hasNotBeenSentTo(uri.toString()))
            .toList();

    if (resolvedUris.isEmpty()) {
      logger.debug("Empty resolved URIs for message: {}", message.getId());
      return Collections.emptySet();
    } else {
      return resolvedUris.stream()
          .map(uri -> requestFactory.buildRequest(message, uri, headers))
          .map(this::processResponse)
          .collect(Collectors.toSet());
    }
  }

  private CompletableFuture<List<SingleMessageSendingResult>> mergeResults(
      Set<CompletableFuture<SingleMessageSendingResult>> results) {
    return CompletableFuture.allOf(results.toArray(new CompletableFuture[results.size()]))
        .thenApply(
            v ->
                results.stream()
                    .map(CompletableFuture::join)
                    .reduce(
                        ImmutableList.<SingleMessageSendingResult>builder(),
                        (builder, element) -> builder.add(element),
                        (listA, listB) -> listA.addAll(listB.build()))
                    .build());
  }

  private CompletableFuture<SingleMessageSendingResult> processResponse(Request request) {
    return resilientMessageSender.send(
        resultFuture ->
            request.send(sendingResultHandlers.handleSendingResultForBroadcast(resultFuture)),
        exceptionMapper);
  }

  @Override
  public void stop() {}
}
