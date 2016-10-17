package pl.allegro.tech.hermes.management.domain.subscription;

import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.filtering.MessageFilter;
import pl.allegro.tech.hermes.common.filtering.MessageFilters;
import pl.allegro.tech.hermes.common.filtering.chain.FilterChain;
import pl.allegro.tech.hermes.common.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.common.message.MessageContent;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;
import pl.allegro.tech.hermes.management.domain.message.filtering.FilteringConversionException;
import pl.allegro.tech.hermes.api.FilterValidation;
import pl.allegro.tech.hermes.management.domain.message.filtering.InvalidFilterTypeException;
import pl.allegro.tech.hermes.api.MessageValidationWrapper;
import tech.allegro.schema.json2avro.converter.AvroConversionException;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static jersey.repackaged.com.google.common.collect.ImmutableMap.of;

@Component
public class FilteringService {
    private final MessageFilters messageFilters;
    private final SchemaRepository schemaRepository;
    private final JsonAvroConverter jsonAvroConverter;


    @Autowired
    public FilteringService(MessageFilters messageFilters, SchemaRepository schemaRepository) {
        this.messageFilters = messageFilters;
        this.schemaRepository = schemaRepository;
        jsonAvroConverter = new JsonAvroConverter();
    }

    public FilterValidation isFiltered(MessageValidationWrapper wrapper, Topic topic) {
        final MessageContent message = getMessageContent(wrapper, topic);

        return isFiltered(message, wrapper.getFilterSpecifications());
    }

    private MessageContent getMessageContent(MessageValidationWrapper wrapper, Topic topic) {
        CompiledSchema<Schema> schema = null;
        byte[] bytes = null;
        switch (topic.getContentType()) {
            case JSON:
                bytes = wrapper.getMessage().getBytes();
                break;
            case AVRO:
                try {
                    if (Optional.ofNullable(wrapper.getSchemaVersion()).isPresent()) {
                        schema = schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(wrapper.getSchemaVersion()));
                    } else {
                        schema = schemaRepository.getAvroSchema(topic);
                    }
                    bytes = jsonAvroConverter.convertToAvro(wrapper.getMessage().getBytes(), schema.getSchema());
                } catch (AvroConversionException e) {
                    throw new FilteringConversionException("Could not convert to avro message. Please check schema version");
                }
                break;
        }

        return new MessageContent.Builder()
                .withData(bytes)
                .withContentType(topic.getContentType())
                .withSchema(Optional.ofNullable(schema))
                .build();
    }

    private FilterValidation isFiltered(MessageContent message, List<MessageFilterSpecification> filterSpecifications) {
        final List<MessageFilter> filters = filterSpecifications.stream()
                .map(messageFilters::compile)
                .collect(toList());

        final FilterResult result = new FilterChain(filters).apply(message);

        if(result.getCause().isPresent()) {
            throw new InvalidFilterTypeException("Filters don't match topic's content type");
        }

        return new FilterValidation(result.isFiltered());
    }
}
