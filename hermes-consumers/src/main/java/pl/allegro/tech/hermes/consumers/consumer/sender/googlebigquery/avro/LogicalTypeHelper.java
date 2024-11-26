package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery.avro;

import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

public class LogicalTypeHelper {
    public static LogicalType fromSchema(Schema schema) {
        String logicalTypeName = schema.getProp(LogicalType.LOGICAL_TYPE_PROP);
        if (logicalTypeName == null) { return null;}
        LogicalType logicalType;
        switch (logicalTypeName) {
            case "decimal":
                logicalType = getDecimal(schema);
                break;
            default:
                logicalType = LogicalTypes.fromSchema(schema);
        }

        return logicalType;
    }

    private static LogicalType getDecimal(Schema schema) {
        int precision = schema.getObjectProps().get("precision") != null ? Integer.parseInt(schema.getObjectProps().get("precision").toString()) : 0;
        int scale = schema.getObjectProps().get("scale") != null ? Integer.parseInt(schema.getObjectProps().get("scale").toString()) : 0;
        return LogicalTypes.decimal(precision, scale);

    }
}
