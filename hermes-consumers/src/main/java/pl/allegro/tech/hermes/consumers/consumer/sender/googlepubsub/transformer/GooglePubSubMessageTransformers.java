package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer;

import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.config.GooglePubSubCompressorProperties;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTarget;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.CompressionCodec;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.CompressionCodecFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression.GooglePubSubMessageCompressor;
import pl.allegro.tech.hermes.consumers.consumer.trace.MetadataAppender;

import java.util.Optional;

public class GooglePubSubMessageTransformers {

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessageTransformers.class);

    private final GooglePubSubCompressorProperties compressorProperties;
    private final GooglePubSubMessageTransformer rawMessageTransformer;

    public GooglePubSubMessageTransformers(MetadataAppender<PubsubMessage> metadataAppender,
                                           GooglePubSubCompressorProperties compressorProperties) {
        this.compressorProperties = compressorProperties;
        this.rawMessageTransformer = new GooglePubSubMessageTransformer(metadataAppender);
    }

    public GooglePubSubMessageTransformer createMessageTransformer(GooglePubSubSenderTarget pubSubTarget) {
        if (pubSubTarget.isCompressionRequested()) {
            Optional<GooglePubSubMessageTransformer> compressingTransformer =
                    createCompressingTransformer(pubSubTarget.getCompressionCodec());
            if (compressingTransformer.isPresent()) {
                return compressingTransformer.get();
            } else {
                logger.warn("PubSub message compression is disabled, switching to raw transfer for {}.", pubSubTarget.getTopicName());
            }
        }
        return rawMessageTransformer;
    }

    private Optional<GooglePubSubMessageTransformer> createCompressingTransformer(CompressionCodec codec) {
        if (compressorProperties.isEnabled()) {
            Optional<CompressionCodecFactory> codecFactory = CompressionCodecFactory.builder()
                    .withCodec(codec)
                    .withCompressionLevel(compressorProperties.getCompressionLevel())
                    .build();

            return codecFactory.map(cf -> new GooglePubSubMessageCompressionTransformer(
                    new GooglePubSubMetadataCompressionAppender(codec),
                    new GooglePubSubMessageCompressor(cf),
                    rawMessageTransformer,
                    compressorProperties.getCompressionThresholdBytes()
                    ));
        } else {
            return Optional.empty();
        }
    }
}
