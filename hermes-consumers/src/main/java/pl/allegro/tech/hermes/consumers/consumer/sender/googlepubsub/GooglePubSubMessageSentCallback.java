package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.api.core.ApiFutureCallback;
import com.google.api.gax.rpc.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.util.concurrent.CompletableFuture;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult;

class GooglePubSubMessageSentCallback implements ApiFutureCallback<String> {
    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessageSentCallback.class);

    private final Message message;
    private final CompletableFuture<MessageSendingResult> resultFuture;

    public GooglePubSubMessageSentCallback(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        this.message = message;
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
