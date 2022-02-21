package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.api.core.ApiFutureCallback;
import com.google.api.gax.rpc.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;

import java.util.concurrent.CompletableFuture;

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult;
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult;

class MessageSentCallback implements ApiFutureCallback<String> {
    private static final Logger logger = LoggerFactory.getLogger(MessageSentCallback.class);

    private final Message message;
    private final CompletableFuture<MessageSendingResult> resultFuture;

    public MessageSentCallback(Message message, CompletableFuture<MessageSendingResult> resultFuture) {
        this.message = message;
        this.resultFuture = resultFuture;
    }

    @Override
    public void onFailure(Throwable throwable) {
        StringBuilder exceptionMessage = new StringBuilder("Error while publishing message to PubSub: ");
        exceptionMessage.append(message);
        if (throwable instanceof ApiException) {
            ApiException apiException = ((ApiException) throwable);
            exceptionMessage.append(apiException.getStatusCode().getCode());
            exceptionMessage.append(apiException.isRetryable());
        }
        logger.warn(exceptionMessage.toString());
        resultFuture.complete(failedResult(throwable));
    }

    @Override
    public void onSuccess(String messageId) {
        resultFuture.complete(succeededResult());
        logger.debug("Published message ID to PubSub: " + messageId);
    }
}
