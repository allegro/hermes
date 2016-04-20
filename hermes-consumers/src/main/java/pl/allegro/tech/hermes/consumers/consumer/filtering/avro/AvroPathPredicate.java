package pl.allegro.tech.hermes.consumers.consumer.filtering.avro;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteringException;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;
import scala.collection.immutable.List;
import wandou.avpath.Evaluator;
import wandou.avpath.Parser;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.consumers.consumer.filtering.FilteringException.check;

public class AvroPathPredicate implements Predicate<Message> {
    private Parser.PathSyntax pathSyntax;
    private Pattern pattern;

    public AvroPathPredicate(Parser.PathSyntax pathSyntax, Pattern pattern) {
        this.pathSyntax = pathSyntax;
        this.pattern = pattern;
    }

    @Override
    public boolean test(final Message message) {
        check(message.getContentType() == ContentType.AVRO, "This filter supports only AVRO contentType.");
        try {
            return matches(selectNodes(message));
        } catch (Exception exception) {
            throw new FilteringException(exception);
        }
    }

    private List<Evaluator.Ctx> selectNodes(final Message message) throws IOException {
        CompiledSchema<Schema> compiledSchema = message.<Schema>getSchema().get();
        return Evaluator.select(bytesToRecord(message.getData(), compiledSchema.getSchema()), pathSyntax);
    }

    private boolean matches(final List<Evaluator.Ctx> nodes) {
        scala.collection.Iterator<Evaluator.Ctx> iter = nodes.iterator();
        while (iter.hasNext()) {
            if (!matches(iter.next().value())) {
                return false;
            }
        }
        return !nodes.isEmpty();
    }

    private boolean matches(Object value) {
        Collection<?> coll = value instanceof Collection? (Collection) value : singletonList(value);
        return coll.stream()
                .map(Objects::toString)
                .allMatch(o -> pattern.matcher(o).matches());
    }
}
