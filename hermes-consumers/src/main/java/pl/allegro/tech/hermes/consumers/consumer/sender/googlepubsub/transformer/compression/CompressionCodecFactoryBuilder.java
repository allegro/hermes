package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class CompressionCodecFactoryBuilder {
    private CompressionCodec codec;
    private CompressionCodecFactory.CompressionLevel compressionLevel;

    private static final Logger logger = LoggerFactory.getLogger(CompressionCodecFactoryBuilder.class);


    public CompressionCodecFactoryBuilder withCodec(CompressionCodec codec) {
        this.codec = codec;
        return this;
    }

    public CompressionCodecFactoryBuilder fromCodecName(String codecName) {
        try {
            this.codec = Optional.ofNullable(codecName)
                    .map(String::toUpperCase)
                    .map(CompressionCodec::valueOf)
                    .orElse(CompressionCodec.EMPTY);
        } catch (IllegalArgumentException ex) {
            logger.warn("Unsupported codec name: {}, turning compression off.", codecName);
            this.codec = CompressionCodec.EMPTY;
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
        return Optional.ofNullable(getCodecFactory(codec));
    }

    private CompressionCodecFactory getCodecFactory(CompressionCodec codec) {
        switch (codec) {
            case DEFLATE: return new CompressionCodecFactory.DeflateCodecFactory(codec.name(), this.compressionLevel.getLevelId());
            case BZIP2: return new CompressionCodecFactory.Bzip2CodecFactory(codec.name());
            case ZSTANDARD: return new CompressionCodecFactory.ZstandardCodecFactory(codec.name(), this.compressionLevel.getLevelId());
            default: return null;
        }
    }
}
