package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.CompressionCodecFactory.SupportedCodec;
import java.util.Optional;

public class CompressionCodecFactoryBuilder {
    private String codecName;
    private int compressionLevel;

    private static final Logger logger = LoggerFactory.getLogger(CompressionCodecFactoryBuilder.class);

    public CompressionCodecFactoryBuilder fromCodecName(String codecName) {
        this.codecName = codecName;
        return this;
    }

    public CompressionCodecFactoryBuilder withCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
        return this;
    }

    public Optional<CompressionCodecFactory> build() {
        try {
            return Optional.ofNullable(this.codecName)
                    .map(String::toUpperCase)
                    .map(SupportedCodec::valueOf)
                    .map(this::getCodecFactory);
        } catch (IllegalArgumentException ex) {
            logger.warn("Unsupported codec name: {}, turning compression off.", this.codecName);
            return Optional.empty();
        }
    }

    private CompressionCodecFactory getCodecFactory(SupportedCodec codec) {
        switch (codec) {
            case DEFLATE: return new CompressionCodecFactory.DeflateCodecFactory(this.codecName, this.compressionLevel);
            case BZIP2: return new CompressionCodecFactory.Bzip2CodecFactory(this.codecName);
            case ZSTANDARD: return new CompressionCodecFactory.ZstandardCodecFactory(this.codecName, this.compressionLevel);
            default: return null;
        }
    }
}
