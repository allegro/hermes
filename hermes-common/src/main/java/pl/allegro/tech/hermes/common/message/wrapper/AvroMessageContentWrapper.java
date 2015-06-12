package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.io.BinaryData;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;

import java.io.IOException;

public class AvroMessageContentWrapper implements MessageContentWrapper {

    private static final int MAXIMUM_METADATA_BYTE_SIZE = 56;

    @Override
    public MessageWithMetadata unwrapContent(byte[] data) {
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);

        try {
            return new MessageWithMetadata(
                new MessageMetadata(binaryDecoder.readLong(), binaryDecoder.readString()),
                binaryDecoder.readBytes(null).array());
        } catch (IOException exception) {
            throw new UnwrappingException("Could not read hermes avro message", exception);
        }
    }

    @Override
    public byte[] wrapContent(byte[] message, String id, long timestamp) {
        byte [] metadataBuffer = new byte[MAXIMUM_METADATA_BYTE_SIZE];

        try {

            int timestampByteSize = BinaryData.encodeLong(timestamp, metadataBuffer, 0);
            int idByteSize = BinaryData.encodeInt(id.length(), metadataBuffer, timestampByteSize);
            System.arraycopy(id.getBytes(), 0, metadataBuffer, timestampByteSize + idByteSize, id.length());
            int messageByteSize = BinaryData.encodeInt(message.length, metadataBuffer, timestampByteSize + idByteSize + id.length());

            int metadataSize = timestampByteSize + idByteSize + id.length() + messageByteSize;
            int wrappedMessageSize = metadataSize + message.length;

            byte [] wrappedMessage = new byte[wrappedMessageSize];
            System.arraycopy(metadataBuffer, 0, wrappedMessage, 0, metadataSize);
            System.arraycopy(message, 0, wrappedMessage, metadataSize, message.length);

            return wrappedMessage;

        } catch (RuntimeException exception) {
            throw new WrappingException("Could not wrap avro message", exception);
        }
    }

}
