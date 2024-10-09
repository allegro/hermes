package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.cloud.bigquery.storage.v1.TableSchema;
import com.google.cloud.bigquery.storage.v1.ToProtoConverter;
import com.google.protobuf.*;
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
            convertSimpleField(inputObject, field, messageBuilder);
        });

        return messageBuilder.build();
    }

    private void convertSimpleField(GenericRecord inputObject, Descriptors.FieldDescriptor field, Message.Builder messageBuilder) {
        String fieldName = field.getName();
        Object fieldValue = inputObject.get(fieldName);

        if (fieldValue != null) {
            if (field.isRepeated()) {
                if (!(fieldValue instanceof Map<?, ?>)) {
                    for (Object el : (Iterable<?>) fieldValue) {
                        messageBuilder.addRepeatedField(field, toProtobufValue(field, el));
                    }
                } else {
                    for (Map.Entry<?, ?> el : ((Map<?, ?>) fieldValue).entrySet()) {
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
    }


    private <T extends Number> T toNumber(Object object, Class<T> clazz) {
        if (object instanceof Number) {
            return numberToSpecific((Number) object, clazz);
        } else if (object instanceof String) {
            return numberToSpecific(Double.parseDouble((String) object), clazz);
        } else if (object instanceof Boolean) {
            return numberToSpecific((Boolean) object ? 1 : 0, clazz);
        } else return clazz.cast(object);

    }

    private <T extends Number> T numberToSpecific(Number object, Class<T> clazz) {
        if (clazz == Integer.class) {
            return clazz.cast((object).intValue());
        } else if (clazz == Long.class) {
            return clazz.cast((object).longValue());
        } else if (clazz == Float.class) {
            return clazz.cast((object).floatValue());
        } else if (clazz == Double.class) {
            return clazz.cast((object).doubleValue());
        } else return clazz.cast(object);
    }

    private Object toProtobufValue(Descriptors.FieldDescriptor fieldDescriptor, Object value) {
        switch (fieldDescriptor.getType()) {
            case UINT32:
            case INT32:
            case SINT32:
            case SFIXED32:
                return toNumber(value, Integer.class);
            case INT64:
            case SINT64:
            case FIXED64:
            case UINT64:
            case SFIXED64:
                return toNumber(value, Long.class);
            case FLOAT:
                return toNumber(value, Float.class);
            case DOUBLE:
                return toNumber(value, Double.class);
            case STRING:
                return value.toString();
            case BOOL:
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                } else
                    return Boolean.parseBoolean(value.toString());

            case ENUM:
                Descriptors.EnumDescriptor enumDescriptor = fieldDescriptor.getEnumType();
                return enumDescriptor.findValueByName((String) value);
            case BYTES:
                ByteBuffer byteBuffer = (ByteBuffer) value;
                return ByteString.copyFrom(byteBuffer);
            case FIXED32:
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
