package pl.allegro.tech.hermes.consumers.consumer.sender.http;

import static com.google.common.base.Preconditions.checkState;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageBatchSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.headers.DefaultBatchHeadersProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.resolver.SimpleEndpointAddressResolver;

public class HttpMessageBatchSenderFactory implements MessageBatchSenderFactory {

  private final SendingResultHandlers resultHandlers;
  private final BatchHttpRequestFactory batchHttpRequestFactory;

  public HttpMessageBatchSenderFactory(
      SendingResultHandlers resultHandlers, BatchHttpRequestFactory batchHttpRequestFactory) {
    this.resultHandlers = resultHandlers;
    this.batchHttpRequestFactory = batchHttpRequestFactory;
  }

  @Override
  public MessageBatchSender create(Subscription subscription) {
    checkState(
        subscription.getEndpoint().getProtocol().contains("http"),
        "Batching is only supported for http/s currently.");

    return new JettyMessageBatchSender(
        batchHttpRequestFactory,
        new SimpleEndpointAddressResolver(),
        resultHandlers,
        new DefaultBatchHeadersProvider());
  }
}
