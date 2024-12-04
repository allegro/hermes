package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Conversion;
import org.apache.avro.Conversions;
import org.apache.avro.LogicalType;
import org.apache.avro.Schema;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

class DecimalToStringConversion extends Conversion<String> {
    private final Conversions.DecimalConversion decimalConversion = new Conversions.DecimalConversion();

    @Override
    public Class<String> getConvertedType() {
        return String.class;
    }

    @Override
    public String fromBytes(ByteBuffer value, Schema schema, LogicalType type) {
        return decimalConversion.fromBytes(value, schema, type).toString();
    }

    @Override
    public ByteBuffer toBytes(String value, Schema schema, LogicalType type) {
        return decimalConversion.toBytes(new BigDecimal(value), schema, type);
    }

    @Override
    public String getLogicalTypeName() {
        return "decimal";
    }
}
