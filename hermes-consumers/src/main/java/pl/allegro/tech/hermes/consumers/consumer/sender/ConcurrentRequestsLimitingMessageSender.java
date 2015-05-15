package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

public abstract class ConcurrentRequestsLimitingMessageSender implements MessageSender {

    @Override
    public ListenableFuture<MessageSendingResult> send(Message message) {
        try {
            final SettableFuture<MessageSendingResult> resultFuture = SettableFuture.create();
            sendMessage(message, resultFuture);
            return resultFuture;
        } catch (Exception e) {
            throw new InternalProcessingException("Failed to send message", e);
        }
    }

    protected abstract void sendMessage(Message message, SettableFuture<MessageSendingResult> resultFuture);

}
