package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GooglePubSubClientsPool {
  private static final Logger logger = LoggerFactory.getLogger(GooglePubSubClientsPool.class);

  private final CredentialsProvider credentialsProvider;
  private final ExecutorProvider publishingExecutorProvider;
  private final RetrySettings retrySettings;
  private final BatchingSettings batchingSettings;
  private final Map<GooglePubSubSenderTarget, GooglePubSubClient> clients = new HashMap<>();
  private final Map<GooglePubSubSenderTarget, Integer> counters = new HashMap<>();

  private final TransportChannelProvider transportChannelProvider;

  GooglePubSubClientsPool(
      CredentialsProvider credentialsProvider,
      ExecutorProvider publishingExecutorProvider,
      RetrySettings retrySettings,
      BatchingSettings batchingSettings,
      TransportChannelProvider transportChannelProvider) {
    this.credentialsProvider = credentialsProvider;
    this.publishingExecutorProvider = publishingExecutorProvider;
    this.retrySettings = retrySettings;
    this.batchingSettings = batchingSettings;
    this.transportChannelProvider = transportChannelProvider;
  }

  synchronized GooglePubSubClient acquire(GooglePubSubSenderTarget resolvedTarget)
      throws IOException {
    GooglePubSubClient client = clients.get(resolvedTarget);
    if (client == null) {
      client = createClient(resolvedTarget);
    }
    clients.put(resolvedTarget, client);
    Integer counter = counters.getOrDefault(resolvedTarget, 0);
    counters.put(resolvedTarget, ++counter);
    return client;
  }

  synchronized void release(GooglePubSubSenderTarget resolvedTarget) {
    Integer counter = counters.getOrDefault(resolvedTarget, 0);
    if (counter == 0) {
      logger.warn("Attempt to release GooglePubSubClient that is not acquired");
    } else if (counter == 1) {
      counters.remove(resolvedTarget);
      GooglePubSubClient client = clients.remove(resolvedTarget);
      client.shutdown();
    } else if (counter > 1) {
      counters.put(resolvedTarget, --counter);
    }
  }

  synchronized void shutdown() {
    clients.values().forEach(GooglePubSubClient::shutdown);
    clients.clear();
    counters.clear();
  }

  protected GooglePubSubClient createClient(GooglePubSubSenderTarget resolvedTarget)
      throws IOException {
    final Publisher.Builder builder =
        Publisher.newBuilder(resolvedTarget.getTopicName())
            .setEndpoint(resolvedTarget.getPubSubEndpoint())
            .setCredentialsProvider(credentialsProvider)
            .setRetrySettings(retrySettings)
            .setBatchingSettings(batchingSettings)
            .setExecutorProvider(publishingExecutorProvider);

    Publisher publisher;
    if (transportChannelProvider == null) {
      publisher = builder.build();
    } else {
      publisher = builder.setChannelProvider(transportChannelProvider).build();
    }
    return new GooglePubSubClient(publisher);
  }
}
