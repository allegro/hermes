package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.nio.ByteBuffer;

import static java.lang.String.format;

public class SchemaAwareSerDe {
    private static final byte MAGIC_BYTE = 0;
    private static final int HEADER_SIZE = 5;

    private SchemaAwareSerDe() {
    }

    public static byte[] serialize(SchemaVersion version, byte[] binaryAvro) {
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + binaryAvro.length);
        buffer.put(MAGIC_BYTE);
        buffer.putInt(version.value());
        buffer.put(binaryAvro);
        return buffer.array();
    }

    public static SchemaAwarePayload deserialize(byte[] payloadWithHeader) {
        ByteBuffer buffer = ByteBuffer.wrap(payloadWithHeader);
        assertMagicByte(buffer.get());
        int schemaVersion = buffer.getInt();
        byte[] payload = new byte[payloadWithHeader.length - HEADER_SIZE];
        buffer.get(payload);
        return new SchemaAwarePayload(payload, SchemaVersion.valueOf(schemaVersion));
    }

    private static void assertMagicByte(byte magicByte) {
        if (magicByte != MAGIC_BYTE) {
            throw new DeserializationException(format("Could not deserialize payload, unknown magic byte: \'%s\'.", magicByte));
        }
    }
}
