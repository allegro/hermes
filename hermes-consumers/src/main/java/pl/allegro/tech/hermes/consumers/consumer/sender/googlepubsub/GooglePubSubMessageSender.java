package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;

public class GooglePubSubMessageSender extends CompletableFutureAwareMessageSender {

    private final GooglePubSubClient googlePubSubClient;

    public GooglePubSubMessageSender(GooglePubSubClient client) {
        this.googlePubSubClient = client;
    }

    @Override
    protected void sendMessage(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        try {
            googlePubSubClient.publish(message, resultFuture);
        } catch (IOException | ExecutionException | InterruptedException exception) {
            resultFuture.complete(failedResult(exception));
        }
    }

    @Override
    public void stop() {
    }
}
