package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.transformer.compression;

import org.apache.avro.file.Codec;

import java.io.IOException;
import java.nio.ByteBuffer;

public class GooglePubSubMessageCompressor {

    private final CompressionCodecFactory codecFactory;

    public GooglePubSubMessageCompressor(CompressionCodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    public byte[] compress(byte[] data) throws IOException {
        Codec codec = codecFactory.createInstance();

        ByteBuffer compressed = codec.compress(ByteBuffer.wrap(data));

        return compressed.array();
    }

    public byte[] decompress(byte[] data) throws IOException {
        Codec codec = codecFactory.createInstance();

        ByteBuffer decompressed = codec.decompress(ByteBuffer.wrap(data));

        return decompressed.array();
    }
}
