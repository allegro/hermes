package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;

public class PubSubMessageSender extends CompletableFutureAwareMessageSender {

    private final PubSubClient pubSubClient;

    public PubSubMessageSender(PubSubClient client) {
        this.pubSubClient = client;
    }

    @Override
    protected void sendMessage(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        try {
            pubSubClient.publish(message, resultFuture);
        } catch (IOException | ExecutionException | InterruptedException exception) {
            resultFuture.complete(failedResult(exception));
        }
    }

    @Override
    public void stop() {
    }
}
