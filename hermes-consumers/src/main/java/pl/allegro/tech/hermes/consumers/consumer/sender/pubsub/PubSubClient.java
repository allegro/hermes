package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.PubsubMessage;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PubSubClient {

    private final Publisher publisher;
    private final PubSubMessages messageCreator;

    PubSubClient(Publisher publisher, PubSubMessages messageCreator) {
        this.publisher = publisher;
        this.messageCreator = messageCreator;
    }

    public void publish(Message message, CompletableFuture<MessageSendingResult> resultFuture)
            throws IOException, ExecutionException, InterruptedException {
        PubsubMessage pubsubMessage = messageCreator.fromHermesMessage(message);
        ApiFuture<String> future = publisher.publish(pubsubMessage);
        ApiFutures.addCallback(future, new MessageSentCallback(message, resultFuture), MoreExecutors.directExecutor());
    }
}
