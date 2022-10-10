package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.io.IOException;

public class GooglePubSubMessagesWithCompression extends GooglePubSubMessages {

    private final GooglePubSubMessageCompressor compressor;
    private final GooglePubSubMessages rawMessages;
    private final Long compressionThresholdBytes;

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessagesWithCompression.class);

    public GooglePubSubMessagesWithCompression(MetadataAppender<PubsubMessage> metadataAppender,
                                               GooglePubSubMessageCompressor compressor,
                                               GooglePubSubMessages rawMessages,
                                               Long compressionThresholdBytes) {
        super(metadataAppender);
        this.compressor = compressor;
        this.rawMessages = rawMessages;
        this.compressionThresholdBytes = compressionThresholdBytes;
    }

    @Override
    public PubsubMessage fromHermesMessage(Message message) {
        try {
            byte[] data = message.getData();
            if (data.length > compressionThresholdBytes) {
                Message compressed = Message.message().fromMessage(message)
                        .withData(compressor.compress(data))
                        .build();
                return super.fromHermesMessage(compressed);
            }
        } catch (IOException e) {
            logger.debug("Error on PubSub message compression, switching to raw message processor", e);
        }
        return rawMessages.fromHermesMessage(message);
    }
}
