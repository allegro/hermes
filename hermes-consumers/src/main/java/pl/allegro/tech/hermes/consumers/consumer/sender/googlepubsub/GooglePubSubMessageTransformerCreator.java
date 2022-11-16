package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class GooglePubSubMessageTransformerCreator {

    private boolean compressionEnabled;
    private Long compressionThresholdBytes;
    private CompressionCodecFactory.CompressionLevel compressionLevel;
    private final GooglePubSubMessageTransformerRaw messageRawTransformer;

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubMessageTransformerCreator.class);

    public static GooglePubSubMessageTransformerCreator creator() {
        return new GooglePubSubMessageTransformerCreator();
    }

    private GooglePubSubMessageTransformerCreator() {
        this.messageRawTransformer = new GooglePubSubMessageTransformerRaw(new GooglePubSubMetadataAppender());
    }

    public GooglePubSubMessageTransformerCreator withCompressionEnabled(boolean compressionEnabled) {
        this.compressionEnabled = compressionEnabled;
        return this;
    }

    public GooglePubSubMessageTransformerCreator withCompressionLevel(String compressionLevel) {
        try {
            this.compressionLevel = Optional.ofNullable(compressionLevel)
                    .map(String::toUpperCase)
                    .map(CompressionCodecFactory.CompressionLevel::valueOf)
                    .orElse(CompressionCodecFactory.CompressionLevel.MEDIUM);
        } catch (IllegalArgumentException ex) {
            logger.warn("Unsupported compression level: {}, setting it to default", compressionLevel);
            this.compressionLevel = CompressionCodecFactory.CompressionLevel.MEDIUM;
        }
        return this;
    }

    public GooglePubSubMessageTransformerCreator withCompressionThresholdBytes(Long compressionThresholdBytes) {
        this.compressionThresholdBytes = compressionThresholdBytes;
        return this;
    }

    GooglePubSubMessageTransformer rawTransformer() {
        return this.messageRawTransformer;
    }

    Optional<GooglePubSubMessageTransformer> transformerForCodec(CompressionCodec codec) {
        if (this.compressionEnabled) {
            return Optional.ofNullable(CompressionCodecFactory.of(codec, compressionLevel))
                    .map(codecFactory ->
                            new GooglePubSubMessageTransformerCompression(
                                    new GooglePubSubMetadataCompressionAppender(codec),
                                    new MessageCompressor(codecFactory)))
                    .map(compressionTransformer ->
                            new GooglePubSubMessageTransformerOptionalCompression(
                                    compressionThresholdBytes,
                                    messageRawTransformer,
                                    compressionTransformer));
        } else {
            return Optional.empty();
        }
    }
}
