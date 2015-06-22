package pl.allegro.tech.hermes.common.message.wrapper;

import org.apache.avro.io.BinaryData;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;

import java.io.IOException;

public class AvroMessageContentWrapper implements MessageContentWrapper {

    @Override
    public UnwrappedMessageContent unwrapContent(byte[] data) {
        BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(data, null);

        try {
            return new UnwrappedMessageContent(
                new MessageMetadata(binaryDecoder.readLong(), binaryDecoder.readString()),
                binaryDecoder.readBytes(null).array());
        } catch (IOException exception) {
            throw new UnwrappingException("Could not read hermes avro message", exception);
        }
    }

    @Override
    public byte[] wrapContent(byte[] message, String id, long timestamp) {
        /*
            the metadata buffer contains timestamp(long), id(string) and size of content(int)
            in avro long has max 10 bytes, string is composed from two types (int for the size and byte array for chars). int has maximum 5 bytes
            Summarizing maximum metadata byte size is 10 + 5 + id.length() + 5
        */
        byte [] metadataBuffer = new byte[20 + id.length()];

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
