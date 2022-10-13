package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression;

import org.apache.avro.file.*;
import org.apache.avro.file.Codec;


public abstract class CompressionCodecFactory {

    public enum CompressionLevel {
        LOW(1),
        MEDIUM(3),
        HIGH(9);

        private final int levelId;

        CompressionLevel(int levelId) {
            this.levelId = levelId;
        }

        public int getLevelId() {
            return levelId;
        }
    }

    private final String name;

    public abstract Codec createInstance();

    public CompressionCodecFactory(String name) {
        this.name = name;
    }

    public String codecName() {
        return this.name;
    }

    public static CompressionCodecFactoryBuilder builder() {
        return new CompressionCodecFactoryBuilder();
    }

    public static class DeflateCodecFactory extends CompressionCodecFactory {

        private final int compressionLevel;

        public DeflateCodecFactory(String name, int compressionLevel) {
            super(name);
            this.compressionLevel = compressionLevel;
        }

        @Override
        public Codec createInstance() {
            return new DeflateCodec(this.compressionLevel);
        }
    }

    public static class Bzip2CodecFactory extends CompressionCodecFactory {

        public Bzip2CodecFactory(String name) {
            super(name);
        }

        @Override
        public Codec createInstance() {
            return new BZip2Codec();
        }
    }

    public static class ZstandardCodecFactory extends CompressionCodecFactory {

        private final int compressionLevel;

        public ZstandardCodecFactory(String name, int compressionLevel) {
            super(name);
            this.compressionLevel = compressionLevel;
        }

        @Override
        public Codec createInstance() {
            return new ZstandardCodec(compressionLevel, true);
        }
    }
}
