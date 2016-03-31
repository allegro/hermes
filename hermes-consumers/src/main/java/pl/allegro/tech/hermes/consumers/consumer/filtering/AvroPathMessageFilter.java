package pl.allegro.tech.hermes.consumers.consumer.filtering;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;
import scala.collection.immutable.List;
import wandou.avpath.Evaluator;
import wandou.avpath.Parser;

import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;

public class AvroPathMessageFilter implements MessageFilter {

    private final Parser.PathSyntax pathSyntax;
    private Pattern pattern;

    public AvroPathMessageFilter(Parser.PathSyntax pathSyntax, Pattern pattern) {
        this.pathSyntax = pathSyntax;
        this.pattern = pattern;
    }

    @Override
    public String type() {
        return "avropath";
    }

    @Override
    public boolean test(final Message message) {
        try {
            CompiledSchema<Schema> compiledSchema = message.<Schema>getSchema().get();
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(message.getData(), null);
            GenericRecord genericRecord = new GenericDatumReader<GenericRecord>(compiledSchema.getSchema()).read(null, binaryDecoder);
            List<Evaluator.Ctx> result = Evaluator.select(genericRecord, pathSyntax);

            if (result.length() == 0) {
                return true;
            } else {
                scala.collection.Iterator iter = result.iterator();
                while (iter.hasNext()) {
                    Evaluator.Ctx ctx = (Evaluator.Ctx) iter.next();
                    if (!evaluate(ctx.value())) {
                        return false;
                    }
                }
            }

            return true;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean evaluate(Object value) {
        if (value instanceof Collection) {
            Collection coll = (Collection) value;
            for (Object elem : coll) {
                if(!evaluate(elem)) {
                    return false;
                }
            }
        } else {
            if (!pattern.matcher(value.toString()).matches()) {
                return false;
            }
        }
        return true;
    }
}
