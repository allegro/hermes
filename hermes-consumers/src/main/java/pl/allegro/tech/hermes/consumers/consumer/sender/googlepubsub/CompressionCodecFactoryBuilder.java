package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.CompressionCodecFactory.SupportedCodec;
import java.util.Optional;

public class CompressionCodecFactoryBuilder {
    private Optional<SupportedCodec> codec;
    private CompressionCodecFactory.CompressionLevel compressionLevel;

    private static final Logger logger = LoggerFactory.getLogger(CompressionCodecFactoryBuilder.class);

    public CompressionCodecFactoryBuilder fromCodecName(String codecName) {
        try {
            this.codec = Optional.ofNullable(codecName)
                    .map(String::toUpperCase)
                    .map(SupportedCodec::valueOf);
        } catch (IllegalArgumentException ex) {
            logger.warn("Unsupported codec name: {}, turning compression off.", codecName);
            this.codec = Optional.empty();
        }
        return this;
    }

    public CompressionCodecFactoryBuilder withCompressionLevel(String compressionLevel) {
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

    public Optional<CompressionCodecFactory> build() {
        return this.codec.map(this::getCodecFactory);
    }

    private CompressionCodecFactory getCodecFactory(SupportedCodec codec) {
        switch (codec) {
            case DEFLATE: return new CompressionCodecFactory.DeflateCodecFactory(codec.name(), this.compressionLevel.getLevelId());
            case BZIP2: return new CompressionCodecFactory.Bzip2CodecFactory(codec.name());
            case ZSTANDARD: return new CompressionCodecFactory.ZstandardCodecFactory(codec.name(), this.compressionLevel.getLevelId());
            default: return null;
        }
    }
}
