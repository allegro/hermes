package pl.allegro.tech.hermes.domain.filtering.avro;

import jakarta.annotation.Nullable;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.domain.filtering.FilterableMessage;
import pl.allegro.tech.hermes.domain.filtering.FilteringException;
import pl.allegro.tech.hermes.domain.filtering.MatchingStrategy;
import pl.allegro.tech.hermes.domain.filtering.UnsupportedMatchingStrategyException;
import pl.allegro.tech.hermes.schema.CompiledSchema;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyListIterator;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.strip;
import static pl.allegro.tech.hermes.common.message.converter.AvroRecordToBytesConverter.bytesToRecord;
import static pl.allegro.tech.hermes.domain.filtering.FilteringException.check;

class AvroPathPredicate implements Predicate<FilterableMessage> {
    private static final String WILDCARD_IDX = "*";
    private static final String GROUP_SELECTOR = "selector";
    private static final String GROUP_IDX = "index";
    private static final String ARRAY_PATTERN_SELECTOR_PART = "(?<" + GROUP_SELECTOR + ">..*)";
    private static final String ARRAY_PATTERN_IDX_PART = "\\[(?<" + GROUP_IDX + ">\\" + WILDCARD_IDX + "|\\d+)]";
    private static final Pattern ARRAY_PATTERN = Pattern.compile(ARRAY_PATTERN_SELECTOR_PART + ARRAY_PATTERN_IDX_PART);
    private static final String NULL_AS_STRING = "null";
    private final List<String> path;
    private final Pattern pattern;
    private final MatchingStrategy matchingStrategy;

    AvroPathPredicate(String path, Pattern pattern, MatchingStrategy matchingStrategy) {
        this.path = Arrays.asList(strip(path, ".").split("\\."));
        this.pattern = pattern;
        this.matchingStrategy = matchingStrategy;
    }

    @Override
    public boolean test(final FilterableMessage message) {
        check(message.getContentType() == ContentType.AVRO, "This filter supports only AVRO contentType.");
        try {
            List<Object> result = select(message);
            Stream<String> resultStream = result.stream().map(Object::toString);

            return !result.isEmpty() && matchResultsStream(resultStream);
        } catch (Exception exception) {
            throw new FilteringException(exception);
        }
    }

    private List<Object> select(final FilterableMessage message) throws IOException {
        CompiledSchema<Schema> compiledSchema = message.getSchema().get();
        return select(bytesToRecord(message.getData(), compiledSchema.getSchema()));
    }

    private List<Object> select(GenericRecord record) {
        ListIterator<String> iter = path.listIterator();
        return select(record, iter);
    }

    private List<Object> select(Object record, ListIterator<String> iter) {
        Object current = record;
        while (iter.hasNext() && isSupportedType(current)) {
            if (current instanceof GenericRecord) {
                GenericRecord currentRecord = (GenericRecord) current;
                String selector = iter.next();
                Matcher arrayMatcher = ARRAY_PATTERN.matcher(selector);

                if (arrayMatcher.matches()) {
                    selector = arrayMatcher.group(GROUP_SELECTOR);

                    current = recordFieldValueOrNull(selector, currentRecord);
                    if (!(current instanceof GenericArray)) {
                        return emptyList();
                    }

                    GenericArray<Object> currentArray = (GenericArray) current;
                    String idx = arrayMatcher.group(GROUP_IDX);

                    if (idx.equals(WILDCARD_IDX)) {
                        return selectMultipleArrayItems(iter, currentArray);
                    } else {
                        current = selectSingleArrayItem(Integer.valueOf(idx), currentArray);
                    }

                } else {
                    current = recordFieldValueOrNull(selector, currentRecord);
                }
            } else if (current instanceof HashMap) {
                Map<Utf8, Object> currentRecord = (HashMap<Utf8, Object>) current;
                Utf8 selector = new Utf8(iter.next());
                current = currentRecord.get(selector);
            }
        }

        return iter.hasNext() ? emptyList() : singletonList(current == null ? NULL_AS_STRING : current);
    }

    private boolean isSupportedType(Object record) {
        return record instanceof GenericRecord || record instanceof HashMap;
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

    private boolean matchResultsStream(Stream<String> results) {
        switch (matchingStrategy) {
            case ALL:
                return results.allMatch(this::matches);
            case ANY:
                return results.anyMatch(this::matches);
            default:
                throw new UnsupportedMatchingStrategyException("avropath", matchingStrategy);
        }
    }

    @Nullable
    private Object recordFieldValueOrNull(String selector, GenericRecord record) {
        Schema.Field field = record.getSchema().getField(selector);
        if (field == null) {
            return null;
        }
        return record.get(field.pos());
    }

    private boolean matches(String value) {
        return pattern.matcher(value).matches();
    }
}
