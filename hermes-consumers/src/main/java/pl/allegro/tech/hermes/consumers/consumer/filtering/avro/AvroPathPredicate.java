package pl.allegro.tech.hermes.consumers.consumer.filtering.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericRecord;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.filtering.FilteringException;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyListIterator;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.StringUtils.strip;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.consumers.consumer.filtering.FilteringException.check;

public class AvroPathPredicate implements Predicate<Message> {
    private static final String WILDCARD_IDX = "*";
    private static final String GROUP_SELECTOR = "selector";
    private static final String GROUP_IDX = "index";
    private static final String ARRAY_PATTERN_SELECTOR_PART = "(?<"+GROUP_SELECTOR+">..*)";
    private static final String ARRAY_PATTERN_IDX_PART = "\\[(?<"+GROUP_IDX+">\\"+ WILDCARD_IDX +"|\\d+)]";
    private static final Pattern ARRAY_PATTERN = Pattern.compile(ARRAY_PATTERN_SELECTOR_PART + ARRAY_PATTERN_IDX_PART);
    private static final String NULL_AS_STRING = "null";
    private List<String> path;
    private Pattern pattern;

    public AvroPathPredicate(String path, Pattern pattern) {
        this.path = Arrays.asList(strip(path, ".").split("\\."));
        this.pattern = pattern;
    }

    @Override
    public boolean test(final Message message) {
        check(message.getContentType() == ContentType.AVRO, "This filter supports only AVRO contentType.");
        try {
            List<Object> result = select(message);

            return !result.isEmpty() && result.stream()
                .map(Object::toString)
                .allMatch(this::matches);
        } catch (Exception exception) {
            throw new FilteringException(exception);
        }
    }

    private List<Object> select(final Message message) throws IOException {
        CompiledSchema<Schema> compiledSchema = message.<Schema>getSchema().get();
        return select(bytesToRecord(message.getData(), compiledSchema.getSchema()));
    }

    private List<Object> select(GenericRecord record) {
        ListIterator<String> iter = path.listIterator();
        return select(record, iter);
    }

    private List<Object> select(Object record, ListIterator<String> iter) {
        Object current = record;
        while (iter.hasNext() && current instanceof GenericRecord) {
            GenericRecord currentRecord = (GenericRecord) current;
            String selector = iter.next();
            Matcher arrayMatcher = ARRAY_PATTERN.matcher(selector);

            if (arrayMatcher.matches()) {
                String idx = arrayMatcher.group(GROUP_IDX);
                selector = arrayMatcher.group(GROUP_SELECTOR);

                current = currentRecord.get(selector);
                if (! (current instanceof GenericArray)) {
                    return emptyList();
                }

                GenericArray<Object> currentArray = (GenericArray) current;
                if (idx.equals(WILDCARD_IDX)) {
                    return selectMultipleArrayItems(iter, currentArray);
                } else {
                    current = selectSingleArrayItem(Integer.valueOf(idx), currentArray);
                }

            } else {
                current = currentRecord.get(selector);
            }
        }

        return iter.hasNext() ? emptyList() : singletonList(current == null ? NULL_AS_STRING : current);
    }

    private List<Object> selectMultipleArrayItems(ListIterator<String> iter, GenericArray<Object> currentArray) {
        return currentArray.stream()
            .map(item -> select(item, iter.hasNext() ? path.listIterator(iter.nextIndex()) : emptyListIterator()))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private Object selectSingleArrayItem(int idx, GenericArray<Object> currentArray) {
        if (currentArray.size() <= idx) {
            return null;
        }

        return currentArray.get(idx);
    }

    private boolean matches(Object value) {
        return pattern.matcher(Objects.toString(value)).matches();
    }
}
