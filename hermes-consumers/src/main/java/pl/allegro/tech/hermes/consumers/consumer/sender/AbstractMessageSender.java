package pl.allegro.tech.hermes.consumers.consumer.sender;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.util.MessageId;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

public abstract class AbstractMessageSender implements MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMessageSender.class);

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

    protected String messageId(Message message) {
        return message.getId().orElseGet(() -> {
            logger.warn("Message for topic {} and offset {} doesn't contain message id. Generating it",
                message.getTopic(), message.getOffset());
            return MessageId.forTopicAndOffset(message.getTopic(), message.getOffset());
        });
    }

}
