package pl.allegro.tech.hermes.consumers.consumer.message;

import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.message.tracker.consumers.MessageMetadata;

public class MessageConverter {

    public static MessageMetadata toMessageMetadata(Message message) {
        return new MessageMetadata(message.getId().get(),
                message.getOffset(),
                message.getPartition(),
                message.getTopic(),
                message.getPublishingTimestamp(),
                message.getReadingTimestamp());
    }
}
