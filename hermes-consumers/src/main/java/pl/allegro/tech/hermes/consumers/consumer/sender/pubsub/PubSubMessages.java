package pl.allegro.tech.hermes.consumers.consumer.sender.pubsub;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

public class PubSubMessages {

    private final MetadataAppender<PubsubMessage> metadataAppender;

    public PubSubMessages(MetadataAppender<PubsubMessage> metadataAppender) {
        this.metadataAppender = metadataAppender;
    }

    public PubsubMessage fromHermesMessage(Message message) {
        final PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(message.getData()))
                .build();

        return metadataAppender.append(pubsubMessage, message);
    }
}
