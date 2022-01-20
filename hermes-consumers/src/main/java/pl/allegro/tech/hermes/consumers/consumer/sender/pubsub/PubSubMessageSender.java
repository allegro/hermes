package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;

public class PubSubMessageSender extends CompletableFutureAwareMessageSender {
    private Subscription subscription;
    private PubSubClient pubSubClient;

    public PubSubMessageSender(Subscription subscription) {
        this.subscription = subscription;
        this.pubSubClient = new PubSubClient("sc-9620-datahub-staging-prod", "hermes-in-pubsub");
    }

    @Override
    protected void sendMessage(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        try {
            pubSubClient.publishingMessage(message, resultFuture);
        } catch (IOException | ExecutionException | InterruptedException exception) {
            resultFuture.complete(failedResult(exception));
        }
    }

    @Override
    public void stop() {

    }
}
