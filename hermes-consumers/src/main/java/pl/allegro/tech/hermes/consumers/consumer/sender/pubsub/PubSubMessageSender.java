package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.util.concurrent.CompletableFuture;

public class PubSubMessageSender extends CompletableFutureAwareMessageSender {
    private Subscription subscription;

    public PubSubMessageSender(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    protected void sendMessage(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        System.out.printf("TRYING TO SEND MESSAGE TO %s\n", subscription.getEndpoint().getEndpoint());
        resultFuture.complete(MessageSendingResult.succeededResult());
    }

    @Override
    public void stop() {

    }
}
