package pl.allegro.tech.hermes.management.domain.topic.schema;

import org.apache.avro.Schema;
import org.apache.avro.compiler.idl.Idl;
import org.apache.avro.compiler.idl.ParseException;

import java.io.ByteArrayInputStream;

public class SchemaConverter {

    public static String convertToAvroSchema(String avroIdl) throws ParseException {
        Idl idl = new Idl(new ByteArrayInputStream(avroIdl.getBytes()));
        Schema schema = idl.CompilationUnit().getTypes().stream().findFirst().orElseThrow(ParseException::new);
        return schema.toString();
    }
}
