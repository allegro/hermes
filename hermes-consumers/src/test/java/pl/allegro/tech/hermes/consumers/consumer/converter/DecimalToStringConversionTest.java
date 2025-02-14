package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

@RunWith(MockitoJUnitRunner.class)
public class DecimalToStringConversionTest {

    private Schema schema;
    private LogicalType logicalType;

    @Before
    public void setup() {
        schema = Schema.create(Schema.Type.BYTES);
        schema.addProp("logicalType", "decimal");
        schema.addProp("precision", 10);
        schema.addProp("scale", 2);
        logicalType = LogicalTypes.fromSchema(schema);
    }

    @Test
    public void toFromBytes() {
        // given
        final String value = "19.91";
        final DecimalToStringConversion conversion = new DecimalToStringConversion();

        //when
        final ByteBuffer byteBuffer = conversion.toBytes(value, schema, logicalType);
        final String result = conversion.fromBytes(byteBuffer, schema, logicalType);

        //then
        Assert.assertEquals(result, value);
    }

    @Test
    public void fromToBytes() {
        // given
        final ByteBuffer value = ByteBuffer.wrap(new BigDecimal("19.91").unscaledValue().toByteArray());
        final DecimalToStringConversion conversion = new DecimalToStringConversion();

        //when
        final String decimal = conversion.fromBytes(value, schema, logicalType);
        final ByteBuffer result = conversion.toBytes(decimal, schema, logicalType);

        //then
        Assert.assertEquals(result, value);
    }

}
