package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

class GooglePubSubClient {

  private static final Logger logger = LoggerFactory.getLogger(GooglePubSubClient.class);

  private final Publisher publisher;

  GooglePubSubClient(Publisher publisher) {
    this.publisher = publisher;
  }

  void publish(PubsubMessage pubsubMessage, CompletableFuture<MessageSendingResult> resultFuture)
      throws IOException, ExecutionException, InterruptedException {
    ApiFuture<String> future = publisher.publish(pubsubMessage);
    ApiFutures.addCallback(
        future, new GooglePubSubMessageSentCallback(resultFuture), MoreExecutors.directExecutor());
  }

  void shutdown() {
    publisher.shutdown();
    try {
      publisher.awaitTermination(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.error("Interrupted termination of the PubSub publisher.");
    }
  }
}
