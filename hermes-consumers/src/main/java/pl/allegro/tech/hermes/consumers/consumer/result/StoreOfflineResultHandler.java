package pl.allegro.tech.hermes.consumers.consumer.result;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;

import java.io.IOException;
import java.util.List;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.config.StoreOfflineResultHandlerConfiguration;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.result.offline.StoreOfflineTranslator;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

public class StoreOfflineResultHandler implements SuccessHandler, ErrorHandler {

  private final Publisher publisher;
  public StoreOfflineResultHandler(
      StoreOfflineResultHandlerConfiguration configuration,
      CredentialsProvider credentialsProvider,
      RetrySettings retrySettings,
      BatchingSettings batchingSettings,
      ExecutorProvider executorProvider
  ) throws IOException {

    this.publisher = Publisher.newBuilder(this.getClass().getName())
            .setEndpoint(configuration.getEndpoint())
            .setCredentialsProvider(credentialsProvider)
            .setRetrySettings(retrySettings)
            .setBatchingSettings(batchingSettings)
            .setExecutorProvider(executorProvider)
            .build();
  }


  private final List<String> supportedSubscriptionNames = List.of("pubsub-consumer", "bigquery-consumer");

  @Override
  public boolean supports(Subscription subscription) {
    return supportedSubscriptionNames.stream()
        .anyMatch(name -> name.equals(subscription.getName()));
  }

  @Override
  public void handleSuccess(
      Message message, Subscription subscription, MessageSendingResult result) {
    PubsubMessage pubsubMessage = StoreOfflineTranslator.translate(message, subscription, result);
    publisher.publish(pubsubMessage);
  }

  @Override
  public void handleDiscarded(
      Message message, Subscription subscription, MessageSendingResult result) {}

  @Override
  public void handleFailed(
      Message message, Subscription subscription, MessageSendingResult result) {}
}
