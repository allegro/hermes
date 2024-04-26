package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import org.apache.avro.file.Codec;

import java.io.IOException;
import java.nio.ByteBuffer;

class MessageCompressor {

    private final CompressionCodecFactory codecFactory;

    MessageCompressor(CompressionCodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    ByteBuffer compress(byte[] data) throws IOException {
        Codec codec = codecFactory.createInstance();

        return codec.compress(ByteBuffer.wrap(data));
    }

    ByteBuffer decompress(byte[] data) throws IOException {
        Codec codec = codecFactory.createInstance();

        return codec.decompress(ByteBuffer.wrap(data));
    }
}
