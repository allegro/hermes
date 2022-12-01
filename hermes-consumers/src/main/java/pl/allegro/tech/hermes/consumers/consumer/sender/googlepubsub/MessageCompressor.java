package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub;

import org.apache.avro.file.Codec;

import java.io.IOException;
import java.nio.ByteBuffer;

class MessageCompressor {

    private final CompressionCodecFactory codecFactory;

    MessageCompressor(CompressionCodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    byte[] compress(byte[] data) throws IOException {
        Codec codec = codecFactory.createInstance();

        ByteBuffer compressed = codec.compress(ByteBuffer.wrap(data));

        return compressed.array();
    }

    byte[] decompress(byte[] data) throws IOException {
        Codec codec = codecFactory.createInstance();

        ByteBuffer decompressed = codec.decompress(ByteBuffer.wrap(data));

        return decompressed.array();
    }
}
