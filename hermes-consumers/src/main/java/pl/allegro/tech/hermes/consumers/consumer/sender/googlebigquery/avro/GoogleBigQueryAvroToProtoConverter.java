package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.cloud.bigquery.storage.v1.TableSchema;
import com.google.cloud.bigquery.storage.v1.ToProtoConverter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Empty;
import org.apache.avro.generic.GenericRecord;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Map;

public class GoogleBigQueryAvroToProtoConverter implements ToProtoConverter<GenericRecord> {
    @Override
    public DynamicMessage convertToProtoMessage(Descriptors.Descriptor protoSchema, TableSchema tableSchema, GenericRecord inputObject, boolean ignoreUnknownFields) {
        return convertToProtoMessage(protoSchema, inputObject);
    }
    public DynamicMessage convertToProtoMessage(Descriptors.Descriptor protoSchema, GenericRecord inputObject) {
        return createMessage(protoSchema, inputObject);
    }

    private DynamicMessage createMessage(Descriptors.Descriptor protoSchema, GenericRecord inputObject) {
        DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(protoSchema);

        protoSchema.getFields().forEach(field -> {
            String fieldName = field.getName();
            Object fieldValue = inputObject.get(fieldName);

            if (fieldValue != null) {
                if (field.isRepeated()) {
                    if (!(fieldValue instanceof Map<?,?>)) {
                        for (Object el : (Iterable<?>) fieldValue) {
                            messageBuilder.addRepeatedField(field, toProtobufValue(field, el));
                        }
                    } else {
                        for (Map.Entry<?, ?> el: ((Map<?, ?>) fieldValue).entrySet()) {
                            DynamicMessage.Builder entryBuilder = DynamicMessage.newBuilder(field.getMessageType());
                            Descriptors.FieldDescriptor valueField = field.getMessageType().findFieldByName("value");
                            entryBuilder.setField(field.getMessageType().findFieldByName("key"), el.getKey().toString());
                            entryBuilder.setField(valueField, toProtobufValue(valueField, el.getValue()));
                            messageBuilder.addRepeatedField(field, entryBuilder.build());
                        }
                    }
                } else {
                    messageBuilder.setField(field, toProtobufValue(field, fieldValue));
                }
            }
        });

        return messageBuilder.build();
    }

    private Object toProtobufValue(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
        switch (fieldDescriptor.getType()) {
            case INT32:
            case SINT32:
            case SFIXED32:
            case INT64:
            case SINT64:
            case SFIXED64:
            case BOOL:
            case FLOAT:
            case DOUBLE:
                return value;
            case STRING:
                return value.toString();
            case ENUM:
                Descriptors.EnumDescriptor enumDescriptor = fieldDescriptor.getEnumType();
                return enumDescriptor.findValueByName((String) value);
            case FIXED64:
            case UINT64:
                BigInteger bigInt = (BigInteger) value;
                return bigInt.longValue();
            case BYTES:
                ByteBuffer byteBuffer = (ByteBuffer) value;
                return ByteString.copyFrom(byteBuffer);
            case FIXED32:
            case UINT32:
                return ((Long) value).intValue();
            case MESSAGE:
                Descriptors.Descriptor messageDescriptor = fieldDescriptor.getMessageType();
                GenericRecord fieldValue = (GenericRecord) value;
                return createMessage(messageDescriptor, fieldValue);
            default:
                throw new RuntimeException("Unsupported field type: " + fieldDescriptor.getType());
        }
    }
}
