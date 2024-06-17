package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import org.apache.avro.file.BZip2Codec;
import org.apache.avro.file.Codec;
import org.apache.avro.file.DeflateCodec;
import org.apache.avro.file.ZstandardCodec;

abstract class CompressionCodecFactory {

    enum CompressionLevel {
        LOW(1),
        MEDIUM(3),
        HIGH(9);

        private final int levelId;

        CompressionLevel(int levelId) {
            this.levelId = levelId;
        }

        int getLevelId() {
            return levelId;
        }
    }

    private final String name;

    static CompressionCodecFactory of(CompressionCodec codec, CompressionLevel level) {
        switch (codec) {
            case DEFLATE: return new DeflateCodecFactory(codec.name(), level.getLevelId());
            case BZIP2: return new CompressionCodecFactory.Bzip2CodecFactory(codec.name());
            case ZSTANDARD: return new CompressionCodecFactory.ZstandardCodecFactory(codec.name(), level.getLevelId());
            default: return null;
        }
    }

    abstract Codec createInstance();

    CompressionCodecFactory(String name) {
        this.name = name;
    }

    String codecName() {
        return this.name;
    }

    static class DeflateCodecFactory extends CompressionCodecFactory {

        private final int compressionLevel;

        DeflateCodecFactory(String name, int compressionLevel) {
            super(name);
            this.compressionLevel = compressionLevel;
        }

        @Override
        Codec createInstance() {
            return new DeflateCodec(this.compressionLevel);
        }
    }


    static class Bzip2CodecFactory extends CompressionCodecFactory {

        Bzip2CodecFactory(String name) {
            super(name);
        }

        @Override
        Codec createInstance() {
            return new BZip2Codec();
        }
    }


    static class ZstandardCodecFactory extends CompressionCodecFactory {

        private final int compressionLevel;

        ZstandardCodecFactory(String name, int compressionLevel) {
            super(name);
            this.compressionLevel = compressionLevel;
        }

        @Override
        Codec createInstance() {
            return new ZstandardCodec(compressionLevel, true, ZstandardCodec.DEFAULT_USE_BUFFERPOOL);
        }
    }
}
