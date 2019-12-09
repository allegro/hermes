package pl.allegro.tech.hermes.common.message.converter;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.ResolvingDecoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class SingleSchemaGenericDatumReader<D> extends GenericDatumReader<D> {

    private static final ThreadLocal<Map<Schema, ResolvingDecoder>> resolvingDecoders = ThreadLocal.withInitial(HashMap::new);
    private final Schema schema;

    SingleSchemaGenericDatumReader(Schema schema) {
        super(schema);
        this.schema = schema;
    }

    @Override
    public D read(D reuse, Decoder in) throws IOException {
        ResolvingDecoder resolver = getResolvingDecoder();
        resolver.configure(in);
        D result = (D) read(reuse, schema, resolver);
        resolver.drain();
        resolver.configure(null);
        return result;
    }

    private ResolvingDecoder getResolvingDecoder() throws IOException {
        Map<Schema, ResolvingDecoder> decodersMap = resolvingDecoders.get();

        ResolvingDecoder decoder = decodersMap.get(schema);
        if (decoder == null) {
            decoder = DecoderFactory.get().resolvingDecoder(Schema.applyAliases(schema, schema), schema, null);
            decodersMap.put(schema, decoder);
        }
        return decoder;
    }
}
