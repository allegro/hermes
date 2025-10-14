package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.cloud.bigquery.storage.v1.TableSchema;
import com.google.cloud.bigquery.storage.v1.ToProtoConverter;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Message;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.apache.avro.generic.GenericRecord;

public class GoogleBigQueryAvroToProtoConverter implements ToProtoConverter<GenericRecord> {
  @Override
  public List<DynamicMessage> convertToProtoMessage(
      Descriptors.Descriptor protoSchema,
      TableSchema tableSchema,
      Iterable<GenericRecord> inputObjects,
      boolean ignoreUnknownFields) {
    return StreamSupport.stream(inputObjects.spliterator(), false)
        .map(it -> convertToProtoMessage(protoSchema, it))
        .toList();
  }

  public DynamicMessage convertToProtoMessage(
      Descriptors.Descriptor protoSchema, GenericRecord inputObject) {
    return createMessage(protoSchema, inputObject);
  }

  private DynamicMessage createMessage(
      Descriptors.Descriptor protoSchema, GenericRecord inputObject) {
    DynamicMessage.Builder messageBuilder = DynamicMessage.newBuilder(protoSchema);

    protoSchema
        .getFields()
        .forEach(
            field -> {
              convertField(inputObject, field, messageBuilder);
            });

    return messageBuilder.build();
  }

  private void convertField(
      GenericRecord inputObject,
      Descriptors.FieldDescriptor field,
      Message.Builder messageBuilder) {
    String fieldName = field.getName();
    if (!inputObject.hasField(fieldName)) {
      return;
    }

    Object fieldValue = inputObject.get(fieldName);
    if (fieldValue == null) {
      return;
    }
    if (field.isRepeated()) {
      if (fieldValue instanceof Map<?, ?>) {
          convertMap(field, messageBuilder, (Map<?, ?>) fieldValue);
      } else {
          convertArray(field, messageBuilder, (Iterable<?>) fieldValue);
      }
    } else {
      messageBuilder.setField(field, toProtobufValue(field, fieldValue));
    }
  }

    private void convertArray(Descriptors.FieldDescriptor field, Message.Builder messageBuilder, Iterable<?> fieldValue) {
        for (Object el : fieldValue) {
          messageBuilder.addRepeatedField(field, toProtobufValue(field, el));
        }
    }

    private void convertMap(Descriptors.FieldDescriptor field, Message.Builder messageBuilder, Map<?, ?> fieldValue) {
        for (Map.Entry<?, ?> el : fieldValue.entrySet()) {
          DynamicMessage.Builder entryBuilder = DynamicMessage.newBuilder(field.getMessageType());
          Descriptors.FieldDescriptor valueField = field.getMessageType().findFieldByName("value");
          entryBuilder.setField(
              field.getMessageType().findFieldByName("key"), el.getKey().toString());
          entryBuilder.setField(valueField, toProtobufValue(valueField, el.getValue()));
          messageBuilder.addRepeatedField(field, entryBuilder.build());
        }
    }

    private <T extends Number> T toNumber(Object object, Class<T> clazz) {
        return switch (object) {
            case Number number -> numberToSpecific(number, clazz);
            case String s -> numberToSpecific(Double.parseDouble(s), clazz);
            case Boolean b -> numberToSpecific(b ? 1 : 0, clazz);
            case null, default -> clazz.cast(object);
        };
  }

  private <T extends Number> T numberToSpecific(Number object, Class<T> clazz) {
      return switch (clazz.getSimpleName()) {
          case "Integer" -> clazz.cast(object.intValue());
          case "Long" -> clazz.cast(object.longValue());
          case "Float" -> clazz.cast(object.floatValue());
          case "Double" -> clazz.cast(object.doubleValue());
          default -> clazz.cast(object);
      };
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
        } else return Boolean.parseBoolean(value.toString());

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
