package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer;

import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.GooglePubSubMessageCompressor;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.io.IOException;

public class GooglePubSubMessageCompressionTransformer extends GooglePubSubMessageTransformer {

    private final GooglePubSubMessageCompressor compressor;
    private final GooglePubSubMessageTransformer rawTransformer;
    private final Long compressionThresholdBytes;

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessageCompressionTransformer.class);

    public GooglePubSubMessageCompressionTransformer(MetadataAppender<PubsubMessage> metadataAppender,
                                                     GooglePubSubMessageCompressor compressor,
                                                     GooglePubSubMessageTransformer rawTransformer,
                                                     Long compressionThresholdBytes) {
        super(metadataAppender);
        this.compressor = compressor;
        this.rawTransformer = rawTransformer;
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
        return rawTransformer.fromHermesMessage(message);
    }
}
