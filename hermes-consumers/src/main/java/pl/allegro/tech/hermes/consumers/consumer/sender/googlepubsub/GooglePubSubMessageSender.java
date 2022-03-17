package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.CompletableFutureAwareMessageSender;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;

class GooglePubSubMessageSender extends CompletableFutureAwareMessageSender {

    private final GooglePubSubClient googlePubSubClient;
    private final GooglePubSubSenderTarget resolvedTarget;
    private final GooglePubSubClientsPool clientsPool;

    GooglePubSubMessageSender(GooglePubSubSenderTarget resolvedTarget,
                                     GooglePubSubClientsPool clientsPool) throws IOException {
        this.googlePubSubClient = clientsPool.acquire(resolvedTarget);
        this.resolvedTarget = resolvedTarget;
        this.clientsPool = clientsPool;
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
        clientsPool.release(resolvedTarget);
    }
}
