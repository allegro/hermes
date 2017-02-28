package pl.allegro.tech.hermes.common.filtering.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.filtering.FilteringException;
import pl.allegro.tech.hermes.common.message.MessageContent;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Optional.empty;
import static org.apache.commons.lang.StringUtils.strip;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.common.filtering.FilteringException.check;

public class AvroPathPredicate implements Predicate<MessageContent> {
    private List<String> path;
    private Pattern pattern;

    public AvroPathPredicate(String path, Pattern pattern) {
        this.path = Arrays.asList(strip(path, ".").split("\\."));
        this.pattern = pattern;
    }

    @Override
    public boolean test(final MessageContent message) {
        check(message.getContentType() == ContentType.AVRO, "This filter supports only AVRO contentType.");
        try {
            return select(message).map(this::matches).orElse(false);
        } catch (Exception exception) {
            throw new FilteringException(exception);
        }
    }

    private Optional<Object> select(final MessageContent message) throws IOException {
        CompiledSchema<Schema> compiledSchema = message.<Schema>getSchema().get();
        return select(bytesToRecord(message.getContent(), compiledSchema.getSchema()));
    }

    private Optional<Object> select(GenericRecord record) {
        Object current = record;
        Iterator<String> iter = path.iterator();
        while (iter.hasNext()) {
            String selector = iter.next();
            current = ((GenericRecord) current).get(selector);
            if (!(current instanceof GenericRecord)) {
                break;
            }
        }
        return iter.hasNext() ? empty() : Optional.ofNullable(current);
    }

    private boolean matches(Object value) {
        return pattern.matcher(Objects.toString(value)).matches();
    }
}
