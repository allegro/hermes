package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import com.google.cloud.bigquery.storage.v1.BigDecimalByteStringEncoder;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.bigdata.avro.gcpavrotools.records.CurrentInstantProvider;
import pl.allegro.bigdata.avro.gcpavrotools.records.InstantProvider;
import pl.allegro.bigdata.avro.gcpavrotools.records.JavaTimeCivilTimeEncoder;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GenericRecordToDynamicMessageConverter {

    private static final Logger logger = LoggerFactory.getLogger(GenericRecordToDynamicMessageConverter.class);
    public static final DateTimeFormatter HOUR_FIELD_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH").withZone(ZoneId.of("Europe/Warsaw"));

    static final Map<Schema.Type, Function<Object, Object>> PRIMITIVE_ENCODERS =
            ImmutableMap.<Schema.Type, Function<Object, Object>>builder()
                    .put(Schema.Type.INT, o -> Long.valueOf((int) o))
                    .put(Schema.Type.FIXED, o -> ByteString.copyFrom(((GenericData.Fixed) o).bytes()))
                    .put(Schema.Type.LONG, Functions.identity())
                    .put(Schema.Type.FLOAT, o -> Double.parseDouble(Float.valueOf((float) o).toString()))
                    .put(Schema.Type.DOUBLE, Function.identity())
                    .put(Schema.Type.STRING, Object::toString)
                    .put(Schema.Type.BOOLEAN, Function.identity())
                    .put(Schema.Type.ENUM, o -> o.toString())
                    .put(Schema.Type.BYTES, o -> {
                        byte[] bytes;
                        if (o instanceof ByteBuffer) {
                            ByteBuffer buffer = (ByteBuffer) o;
                            buffer.rewind();
                            bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                        } else {
                            bytes = (byte[]) o;
                        }
                        return ByteString.copyFrom(bytes);
                    })
                    .build();

    // A map of supported logical types to their encoding functions.
    static final Map<String, BiFunction<LogicalType, Object, Object>> LOGICAL_TYPE_ENCODERS =
            ImmutableMap.<String, BiFunction<LogicalType, Object, Object>>builder()
                    .put(LogicalTypes.date().getName(), (logicalType, value) -> convertDate(value))
                    .put(LogicalTypes.timeMillis().getName(), ((logicalType, o) -> convertTime(o, true)))
                    .put(LogicalTypes.timeMicros().getName(), ((logicalType, o) -> convertTime(o, false)))
                    .put(
                            LogicalTypes.decimal(1).getName(), GenericRecordToDynamicMessageConverter::convertDecimal)
                    .put(
                            LogicalTypes.timestampMicros().getName(),
                            (logicalType, value) -> convertTimestamp(value, true))
                    .put(
                            LogicalTypes.timestampMillis().getName(),
                            (logicalType, value) -> convertTimestamp(value, false))
                    .put(LogicalTypes.uuid().getName(), (logicalType, value) -> convertUUID(value))
                    .build();

    private final InstantProvider instantProvider;

    public GenericRecordToDynamicMessageConverter() {
        instantProvider = new CurrentInstantProvider();
    }

    public GenericRecordToDynamicMessageConverter(InstantProvider instantProvider) {
        this.instantProvider = instantProvider;
    }

    public DynamicMessage messageFromGenericRecord(
            Descriptors.Descriptor descriptor,
            GenericRecord record
    ) {
        Schema schema = record.getSchema();
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);

        for (Schema.Field field : schema.getFields()) {
            // in to original code, field was found by lowercase field.name()
            // as inner columns are case-sensitive in BigQuery, we've removed toLowerCase() call.
            if (field.name().equals("__metadata")) {
                FieldDescriptor metaFieldDescriptor = Preconditions.checkNotNull(descriptor.findFieldByName("__meta"));
                Map<?, ?> metadataField = (Map<?, ?>) (record.get("__metadata"));
                Object metaFieldValue = getMetaFieldValue(metaFieldDescriptor, field, metadataField);
                builder.setField(metaFieldDescriptor, metaFieldValue);
                FieldDescriptor hourFieldDescriptor = Preconditions.checkNotNull(descriptor.findFieldByName("__hour"));
                Object hourFieldValue = getHourFieldValue(metadataField);
                builder.setField(hourFieldDescriptor, hourFieldValue);
            }
            if (descriptor.findFieldByName(field.name().toLowerCase()) == null) {
                logger.warn("Field {} does not exist", field.name());
            }
            FieldDescriptor fieldDescriptor = Preconditions.checkNotNull(descriptor.findFieldByName(field.name().toLowerCase()));
            Object value = messageValueFromGenericRecordValue(fieldDescriptor, field, field.name(), record, field.getProp(LogicalType.LOGICAL_TYPE_PROP));
            if (value != null) {
                builder.setField(fieldDescriptor, value);
            }
        }
        if (descriptor.findFieldByName("__hour") != null) {
            FieldDescriptor hour = Preconditions.checkNotNull(descriptor.findFieldByName("__hour"));
            if (!builder.hasField(hour)) {
                builder.setField(hour, getHourFieldValue(Maps.newHashMap()));
            }
        }

        return builder.build();
    }

    private Object getHourFieldValue(Map<?, ?> metadataField) {
        final Instant timestamp;
        if (metadataField != null) {
            if (metadataField.containsKey(new Utf8("timestamp"))) {
                timestamp = Instant.ofEpochMilli(
                        Long.parseLong(metadataField.get(new Utf8("timestamp")).toString()));
            } else {
                timestamp = instantProvider.getInstant();
            }
        } else {
            timestamp = instantProvider.getInstant();
        }
        return HOUR_FIELD_DATETIME_FORMATTER.format(timestamp);
    }

    private DynamicMessage getMetaFieldValue(FieldDescriptor metaFieldDescriptor, Schema.Field field, Map<?, ?> record) {
        DynamicMessage.Builder builder = DynamicMessage.newBuilder(metaFieldDescriptor.getMessageType());

        if (record != null && record.containsKey(new Utf8("messageid")) && record.containsKey(new Utf8("timestamp"))) {
            builder.setField(metaFieldDescriptor.getMessageType().findFieldByName("messageid"), record.get(new Utf8("messageId")).toString());
            builder.setField(metaFieldDescriptor.getMessageType().findFieldByName("timestamp"), Long.valueOf(record.get(new Utf8("timestamp")).toString()));
        }
        return builder.build();
    }

    private Object messageValueFromGenericRecordValue(
            FieldDescriptor fieldDescriptor, Schema.Field avroField, String name, GenericRecord record, String inheritedLogicalType) {
        @Nullable Object value = record.get(name);
        if (value == null) {
            if (fieldDescriptor.isOptional() || (fieldDescriptor.isRepeated() && avroField.schema().getType().equals(Schema.Type.UNION))) {
                return null;
            } else {
                throw new IllegalArgumentException(
                        "Received null value for non-nullable field " + fieldDescriptor.getName());
            }
        }
        if (avroField.hasProps() && avroField.getProp(LogicalType.LOGICAL_TYPE_PROP) != null) {
            return toProtoValue(fieldDescriptor, avroField.schema(), avroField.getProp(LogicalType.LOGICAL_TYPE_PROP), value);

        } else {
            return toProtoValue(fieldDescriptor, avroField.schema(), inheritedLogicalType, value);
        }
    }

    private Object toProtoValue(
            FieldDescriptor fieldDescriptor, Schema avroSchema, Object value) {
        return toProtoValue(fieldDescriptor, avroSchema, null, value);
    }
    private Object toProtoValue(
            FieldDescriptor fieldDescriptor, Schema avroSchema, String inheritedLogicalType, Object value) {
        switch (avroSchema.getType()) {
            case RECORD:
                return messageFromGenericRecord(
                        fieldDescriptor.getMessageType(), (GenericRecord) value);
            case ARRAY:
                Iterable<Object> iterable = (Iterable<Object>) value;
                @Nullable Schema arrayElementType = avroSchema.getElementType();
                if (arrayElementType == null) {
                    throw new RuntimeException("Unexpected null element type!");
                }
                return StreamSupport.stream(iterable.spliterator(), false)
                        .map(v -> toProtoValue(fieldDescriptor, arrayElementType, v))
                        .collect(Collectors.toList());
            case UNION:
                TypeWithNullability type;
                if (avroSchema.getTypes().size() == 1) {
                    type = TypeWithNullability.create(avroSchema.getTypes().get(0));
                } else {
                    type = TypeWithNullability.create(avroSchema);
                }
                Preconditions.checkState(
                        type.getType().getType() != Schema.Type.UNION,
                        "Multiple non-null union types are not supported.");
                if (inheritedLogicalType == null && avroSchema.hasProps() && avroSchema.getProp(LogicalType.LOGICAL_TYPE_PROP) != null) {

                    return toProtoValue(fieldDescriptor, type.getType(), avroSchema.getProp(LogicalType.LOGICAL_TYPE_PROP), value);
                } else {
                    return toProtoValue(fieldDescriptor, type.getType(), inheritedLogicalType, value);
                }
            case MAP:
                Map<CharSequence, Object> map = (Map<CharSequence, Object>) value;
                Schema valueType = TypeWithNullability.create(avroSchema.getValueType()).getType();
                if (valueType == null) {
                    throw new RuntimeException("Unexpected null element type!");
                }

                return map.entrySet().stream()
                        .map(
                                (Map.Entry<CharSequence, Object> entry) -> {
                                    Descriptors.Descriptor valueDescriptor;
                                    Schema valueFieldType;
                                    if (/*valueType.getType().equals(Schema.Type.RECORD) ||*/ valueType.getType().equals(Schema.Type.MAP)) {
                                        valueDescriptor = fieldDescriptor.getMessageType().findFieldByName("value").getMessageType();
                                        valueFieldType = valueType.getValueType();
                                    } else {
                                        valueDescriptor = fieldDescriptor.getMessageType();
                                        valueFieldType = valueType;
                                    }
                                    return mapEntryToProtoValue(valueDescriptor, valueFieldType, entry);
                                })
                        .collect(Collectors.toList());
            default:
                return scalarToProtoValue(avroSchema, inheritedLogicalType, value);
        }
    }

    Object mapEntryToProtoValue(
            Descriptors.Descriptor descriptor, Schema valueFieldType, Map.Entry<CharSequence, Object> entryValue) {

        DynamicMessage.Builder builder = DynamicMessage.newBuilder(descriptor);
        FieldDescriptor keyFieldDescriptor =
                Preconditions.checkNotNull(descriptor.findFieldByName("key"));
        @Nullable
        Object key =
                toProtoValue(keyFieldDescriptor, Schema.create(Schema.Type.STRING), entryValue.getKey());
        if (key != null) {
            builder.setField(keyFieldDescriptor, key);
        }
        FieldDescriptor valueFieldDescriptor =
                Preconditions.checkNotNull(descriptor.findFieldByName("value"));
        @Nullable
        Object value = toProtoValue(valueFieldDescriptor, valueFieldType, entryValue.getValue());
        if (value != null) {
            builder.setField(valueFieldDescriptor, value);
        }
        return builder.build();
    }

    Object scalarToProtoValue(Schema fieldSchema, String inheritedLogicalType, Object value) {
        TypeWithNullability type = TypeWithNullability.create(fieldSchema);
        LogicalType logicalType;
        // hack to get around a bug in avro 1.8.1 (it throws NPE if there is no logical type defined)

        if (type.getType().getProp(LogicalType.LOGICAL_TYPE_PROP) == null && inheritedLogicalType != null) {
            type.getType().addProp(LogicalType.LOGICAL_TYPE_PROP, inheritedLogicalType);
        }
//        logicalType = LogicalTypes.fromSchema(type.getType());
        logicalType = LogicalTypeHelper.fromSchema(type.getType());
        if (logicalType != null) {
            @Nullable
            BiFunction<LogicalType, Object, Object> logicalTypeEncoder =
                    LOGICAL_TYPE_ENCODERS.get(logicalType.getName());
            if (logicalTypeEncoder == null) {
                throw new IllegalArgumentException("Unsupported logical type " + logicalType.getName());
            }
            return logicalTypeEncoder.apply(logicalType, value);
        } else {
            @Nullable Function<Object, Object> encoder = PRIMITIVE_ENCODERS.get(type.getType().getType());
            if (encoder == null) {
                throw new RuntimeException("Unexpected type " + fieldSchema);
            }
            if (value == null) {
                return null;
            }
            return encoder.apply(value);
        }
    }

    static Number convertDate(Object value) {
        Preconditions.checkArgument(
                value instanceof Number, "Expecting a value as Integer type (days).");
        return ((Number) value).intValue();
    }

    static String convertUUID(Object value) {
        if (value instanceof UUID) {
            return ((UUID) value).toString();
        } else {
            Preconditions.checkArgument(value instanceof String, "Expecting a value as String type.");
            UUID.fromString((String) value);
            return (String) value;
        }
    }

    static Long convertTimestamp(Object value, boolean micros) {
        // should return micros
        if (value instanceof String) {
            return Long.parseLong(value.toString());
        } else if (value instanceof Number)  {
            return ((Number) value).longValue() * (micros ? 1 : 1000);
        }
        else {
            throw new IllegalArgumentException("Expecting timestamp as string or number");
        }
    }

    private static Object convertTime(Object value, boolean millis) {
        Preconditions.checkArgument(
                value instanceof Long || value instanceof Integer, "Expecting a value as Long or Integer type.");
            return JavaTimeCivilTimeEncoder.encodePacked64TimeMicros(LocalTime.ofNanoOfDay(((Number) value).longValue() * (millis ? 1000000 : 1000)));
    }

    static Object convertDecimal(LogicalType logicalType, Object value) {
        if (value instanceof Utf8 || value instanceof String) {
            return value.toString();
        }
        else if (value instanceof ByteBuffer) {
          return convertDecimalFromByteString(logicalType, value);
        }
        else {
            throw new IllegalArgumentException("Unsupported logical type " + logicalType.getName() + " for " + value + " object");
        }
    }

    static ByteString convertDecimalFromByteString(LogicalType logicalType, Object value) {
        ByteBuffer byteBuffer = (ByteBuffer) value;
        BigDecimal bigDecimal =
                new Conversions.DecimalConversion()
                        .fromBytes(
                                byteBuffer.duplicate(),
                                Schema.create(Schema.Type.NULL), // dummy schema, not used
                                logicalType);
        return BigDecimalByteStringEncoder.encodeToNumericByteString(bigDecimal);
    }

    public static class TypeWithNullability {
        public final Schema type;
        public final boolean nullable;

        public static TypeWithNullability create(Schema avroSchema) {
            return new TypeWithNullability(avroSchema);
        }

        TypeWithNullability(Schema avroSchema) {
            if (avroSchema.getType() == Schema.Type.UNION) {
                List<Schema> types = avroSchema.getTypes();

                // optional fields in AVRO have form of:
                // {"name": "foo", "type": ["null", "something"]}

                // don't need recursion because nested unions aren't supported in AVRO
                List<Schema> nonNullTypes =
                        types.stream().filter(x -> x.getType() != Schema.Type.NULL).collect(Collectors.toList());

                if (nonNullTypes.size() == types.size() || nonNullTypes.isEmpty()) {
                    // union without `null` or all 'null' union, keep as is.
                    type = avroSchema;
                    nullable = false;
                } else if (nonNullTypes.size() > 1) {
                    type = Schema.createUnion(nonNullTypes);
                    nullable = true;
                } else {
                    // One non-null type.
                    type = nonNullTypes.get(0);
                    nullable = true;
                }
            } else {
                type = avroSchema;
                nullable = false;
            }
        }

        public Boolean isNullable() {
            return nullable;
        }

        public Schema getType() {
            return type;
        }
    }
}
