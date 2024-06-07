package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.io.IOException;

class GooglePubSubMessageTransformerCompression implements GooglePubSubMessageTransformer {

    private final MessageCompressor compressor;
    private final Long compressionThresholdBytes;
    private final GooglePubSubMessageTransformerRaw rawTransformer;
    private final MetadataAppender<PubsubMessage> metadataAppender;
    private final SubscriptionLoadRecorder loadRecorder;
    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessageTransformerCompression.class);

    GooglePubSubMessageTransformerCompression(
            Long compressionThresholdBytes,
            GooglePubSubMessageTransformerRaw rawTransformer,
            MetadataAppender<PubsubMessage> metadataAppender,
            MessageCompressor compressor, SubscriptionLoadRecorder loadRecorder) {
        this.compressor = compressor;
        this.compressionThresholdBytes = compressionThresholdBytes;
        this.rawTransformer = rawTransformer;
        this.metadataAppender = metadataAppender;
        this.loadRecorder = loadRecorder;
    }

    @Override
    public PubsubMessage fromHermesMessage(Message message) {
        byte[] data = message.getData();
        if (data.length > compressionThresholdBytes) {
            return compressHermesMessage(message);
        } else {
            return rawTransformer.fromHermesMessage(message);
        }
    }

    private PubsubMessage compressHermesMessage(Message message) {
        try {
            var start = System.nanoTime();
            final PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFrom(
                            compressor.compress(message.getData())
                    )).build();

            var end = System.nanoTime();

            logger.info("Compressed message for topic {} in {} ns", message.getTopic(), end - start);

            loadRecorder.recordSingleOperation();
            return metadataAppender.append(pubsubMessage, message);
        } catch (IOException e) {
            throw new GooglePubSubMessageCompressionException("Error on PubSub message compression", e);
        }
    }
}
