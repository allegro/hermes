package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.cloud.bigquery.storage.v1.TableSchema;
import com.google.cloud.bigquery.storage.v1.ToProtoConverter;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public class AllegroAvroToBigQueryRow implements ToProtoConverter<GenericRecord> {

    private static final Logger logger = LoggerFactory.getLogger(AllegroAvroToBigQueryRow.class);

    public AllegroAvroToBigQueryRow(GenericRecordToDynamicMessageConverter toProtobufConverter) {
        this.toProtobufConverter = toProtobufConverter;

    }
    private final GenericRecordToDynamicMessageConverter toProtobufConverter;

    @Override
    public DynamicMessage convertToProtoMessage(Descriptors.Descriptor descriptor, TableSchema tableSchema, GenericRecord genericRecord, boolean ignoreUnknownFields) {
//        StringBuilder builder = new StringBuilder();
//        for (Descriptors.FieldDescriptor field : descriptor.getFields()) {
//            builder.append(String.format("%s -> %s", field.getName(), field.toProto().))
//        }
        String fields = descriptor.getFields().stream().map(Descriptors.FieldDescriptor::getName).collect(Collectors.joining(", "));
        String fieldTypes = descriptor.getFields().stream().map(f -> f.getName() + ":" + f.getType().name()).collect(Collectors.joining(", "));
        String fieldJavaTypes = descriptor.getFields().stream().map(f -> f.getName() + ":" + f.getJavaType().name()).collect(Collectors.joining(", "));
        logger.info("convertToProtoMessage fields -> {}", fields);
        logger.info("convertToProtoMessage fieldTypes -> {}", fieldTypes);
        logger.info("convertToProtoMessage fieldJavaTypes -> {}", fieldJavaTypes);
        logger.info("convertToProtoMessage __meta fields -> {}", descriptor.findFieldByName("__meta").getMessageType().getFields().stream().map(Descriptors.FieldDescriptor::getName).collect(Collectors.joining(", ")));
        logger.info("convertToProtoMessage __meta.messageId field -> {}", descriptor.findFieldByName("__meta").getMessageType().findFieldByName("messageId"));
        logger.info("convertToProtoMessage __meta.timestamp field -> {}", descriptor.findFieldByName("__meta").getMessageType().findFieldByName("timestamp"));

        return toProtobufConverter.messageFromGenericRecord(descriptor, genericRecord);
    }
}
