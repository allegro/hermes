package pl.allegro.tech.hermes.common.message.wrapper;

import static java.lang.String.format;

import java.nio.ByteBuffer;
import pl.allegro.tech.hermes.schema.SchemaId;

public class SchemaAwareSerDe {
  private static final byte MAGIC_BYTE_VALUE = 0;
  private static final byte MAGIC_BYTE_INDEX = 0;
  private static final int HEADER_SIZE = 5;

  private SchemaAwareSerDe() {}

  public static byte[] serialize(SchemaId schemaId, byte[] binaryAvro) {
    ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + binaryAvro.length);
    buffer.put(MAGIC_BYTE_VALUE);
    buffer.putInt(schemaId.value());
    buffer.put(binaryAvro);
    return buffer.array();
  }

  public static SchemaAwarePayload deserialize(byte[] payloadWithHeader) {
    ByteBuffer buffer = ByteBuffer.wrap(payloadWithHeader);
    assertMagicByte(buffer.get());
    int schemaId = buffer.getInt();
    byte[] payload = new byte[payloadWithHeader.length - HEADER_SIZE];
    buffer.get(payload);
    return new SchemaAwarePayload(payload, SchemaId.valueOf(schemaId));
  }

  public static boolean startsWithMagicByte(byte[] payload) {
    return payload[MAGIC_BYTE_INDEX] == MAGIC_BYTE_VALUE;
  }

  private static void assertMagicByte(byte magicByte) {
    if (magicByte != MAGIC_BYTE_VALUE) {
      throw new DeserializationException(
          format("Could not deserialize payload, unknown magic byte: \'%s\'.", magicByte));
    }
  }

  public static byte[] trimMagicByteAndSchemaVersion(byte[] data) {
    int length = data.length - HEADER_SIZE;
    byte[] dataWithoutMagicByteAndSchemaVersion = new byte[length];
    System.arraycopy(data, HEADER_SIZE, dataWithoutMagicByteAndSchemaVersion, 0, length);
    return dataWithoutMagicByteAndSchemaVersion;
  }
}
