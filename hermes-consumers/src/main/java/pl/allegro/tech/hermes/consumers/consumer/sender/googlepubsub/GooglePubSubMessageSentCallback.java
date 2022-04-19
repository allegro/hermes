package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.api.core.ApiFutureCallback;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.util.concurrent.CompletableFuture;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult;

class GooglePubSubMessageSentCallback implements ApiFutureCallback<String> {
    private final CompletableFuture<MessageSendingResult> resultFuture;

    GooglePubSubMessageSentCallback(CompletableFuture<MessageSendingResult> resultFuture) {
        this.resultFuture = resultFuture;
    }

    @Override
    public void onFailure(Throwable throwable) {
        resultFuture.complete(failedResult(throwable));
    }

    @Override
    public void onSuccess(String messageId) {
        resultFuture.complete(succeededResult());
    }
}
