package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;

import com.google.pubsub.v1.PubsubMessage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

class GooglePubSubMessageSender implements CompletableFutureAwareMessageSender {

  private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessageSender.class);
  private final GooglePubSubClient googlePubSubClient;
  private final GooglePubSubSenderTarget resolvedTarget;
  private final GooglePubSubClientsPool clientsPool;
  private final GooglePubSubMessageTransformer messageTransformer;

  GooglePubSubMessageSender(
      GooglePubSubSenderTarget resolvedTarget,
      GooglePubSubClientsPool clientsPool,
      GooglePubSubMessageTransformer messageTransformer)
      throws IOException {
    logger.info("Acquiring googlePubSubClient for {} for topic {} with credentials {}", resolvedTarget.getPubSubEndpoint(), resolvedTarget.getTopicName(), clientsPool.getCredentialsProvider());
    this.googlePubSubClient = clientsPool.acquire(resolvedTarget);
    this.resolvedTarget = resolvedTarget;
    this.clientsPool = clientsPool;
    this.messageTransformer = messageTransformer;
  }

  @Override
  public void send(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
    try {
      PubsubMessage pubsubMessage = messageTransformer.fromHermesMessage(message);
      googlePubSubClient.publish(pubsubMessage, resultFuture);
    } catch (IOException | ExecutionException | InterruptedException exception) {
      resultFuture.complete(failedResult(exception));
    }
  }

  @Override
  public void stop() {
    clientsPool.release(resolvedTarget);
  }
}
