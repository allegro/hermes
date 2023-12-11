package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.common.base.Preconditions;
import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.GoogleBigQueryMessageTransformer;

public class GoogleBigQueryAvroMessageTransformer implements GoogleBigQueryMessageTransformer<GenericRecord> {
    @Override
    public GenericRecord fromHermesMessage(Message message) {

        Preconditions.checkArgument(message.getContentType().equals(ContentType.AVRO));
        Preconditions.checkArgument(message.getSchema().isPresent());

        return AvroRecordToBytesConverter.bytesToRecord(message.getData(), message.getSchema().get().getSchema());
    }
}
