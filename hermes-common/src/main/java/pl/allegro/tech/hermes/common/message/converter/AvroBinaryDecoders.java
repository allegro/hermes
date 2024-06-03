package pl.allegro.tech.hermes.common.message.converter;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import tech.allegro.schema.json2avro.converter.AvroConversionException;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.InputStream;

public class AvroBinaryDecoders {

    private static ThreadLocal<InputStream> threadLocalEmptyInputStream =
            ThreadLocal.withInitial(() -> new ByteArrayInputStream(new byte[0]));

    private static ThreadLocal<BinaryDecoder> threadLocalBinaryDecoder =
            ThreadLocal.withInitial(() -> DecoderFactory.get().binaryDecoder(threadLocalEmptyInputStream.get(), null));

    static GenericRecord decodeReusingThreadLocalBinaryDecoder(byte[] message, Schema schema) {
        try (FlushableBinaryDecoderHolder holder = new FlushableBinaryDecoderHolder()) {
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(message, holder.getBinaryDecoder());
            return new GenericDatumReader<GenericRecord>(schema).read(null, binaryDecoder);
        } catch (Exception e) {
            String reason = e.getMessage() == null ? ExceptionUtils.getRootCauseMessage(e) : e.getMessage();
            throw new AvroConversionException(String.format("Could not deserialize Avro message with provided schema, reason: %s", reason));
        }
    }

    static class FlushableBinaryDecoderHolder implements Closeable {

        final BinaryDecoder binaryDecoder = threadLocalBinaryDecoder.get();

        BinaryDecoder getBinaryDecoder() {
            return binaryDecoder;
        }

        @Override
        public void close() {
            DecoderFactory.get().binaryDecoder(threadLocalEmptyInputStream.get(), threadLocalBinaryDecoder.get());
        }
    }
}
