package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GooglePubSubMessageTransformerCreator {

  private boolean compressionEnabled;
  private Long compressionThresholdBytes;
  private CompressionCodecFactory.CompressionLevel compressionLevel;
  private final GooglePubSubMessageTransformerRaw messageRawTransformer;

  private static final Logger logger =
      LoggerFactory.getLogger(GooglePubSubMessageTransformerCreator.class);

  public static GooglePubSubMessageTransformerCreator creator() {
    return new GooglePubSubMessageTransformerCreator();
  }

  private GooglePubSubMessageTransformerCreator() {
    this.messageRawTransformer =
        new GooglePubSubMessageTransformerRaw(new GooglePubSubMetadataAppender());
  }

  public GooglePubSubMessageTransformerCreator withCompressionEnabled(boolean compressionEnabled) {
    this.compressionEnabled = compressionEnabled;
    return this;
  }

  public GooglePubSubMessageTransformerCreator withCompressionLevel(String compressionLevel) {
    try {
      this.compressionLevel =
          Optional.ofNullable(compressionLevel)
              .map(String::toUpperCase)
              .map(CompressionCodecFactory.CompressionLevel::valueOf)
              .orElse(CompressionCodecFactory.CompressionLevel.MEDIUM);
    } catch (IllegalArgumentException ex) {
      logger.warn("Unsupported compression level: {}, setting it to default", compressionLevel);
      this.compressionLevel = CompressionCodecFactory.CompressionLevel.MEDIUM;
    }
    return this;
  }

  public GooglePubSubMessageTransformerCreator withCompressionThresholdBytes(
      Long compressionThresholdBytes) {
    this.compressionThresholdBytes = compressionThresholdBytes;
    return this;
  }

  GooglePubSubMessageTransformer getTransformerForTargetEndpoint(
      GooglePubSubSenderTarget pubSubTarget) {
    if (this.compressionEnabled && pubSubTarget.isCompressionRequested()) {
      Optional<GooglePubSubMessageTransformer> compressingTransformer =
          transformerForCodec(pubSubTarget.getCompressionCodec());
      if (compressingTransformer.isPresent()) {
        return compressingTransformer.get();
      } else {
        logger.warn(
            "Unsupported codec, switching to raw transfer for {}.", pubSubTarget.getTopicName());
      }
    }
    return this.messageRawTransformer;
  }

  private Optional<GooglePubSubMessageTransformer> transformerForCodec(CompressionCodec codec) {
    return Optional.ofNullable(codec)
        .map(it -> CompressionCodecFactory.of(it, compressionLevel))
        .map(
            codecFactory ->
                new GooglePubSubMessageTransformerCompression(
                    this.compressionThresholdBytes,
                    this.messageRawTransformer,
                    new GooglePubSubMetadataCompressionAppender(codec),
                    new MessageCompressor(codecFactory)));
  }
}
