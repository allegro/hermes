package pl.allegro.tech.hermes.schema;

import org.apache.avro.Schema;

public interface SchemaCompilersFactory {

    static SchemaCompiler<Schema> avroSchemaCompiler() {
        return source -> new Schema.Parser().parse(source.value());
    }
}
