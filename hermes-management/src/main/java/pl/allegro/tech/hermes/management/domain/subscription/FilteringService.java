package pl.allegro.tech.hermes.management.domain.subscription;

import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.filtering.MessageFilter;
import pl.allegro.tech.hermes.common.filtering.MessageFilters;
import pl.allegro.tech.hermes.common.filtering.NoSuchFilterException;
import pl.allegro.tech.hermes.common.filtering.chain.FilterChain;
import pl.allegro.tech.hermes.common.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.common.message.MessageContent;
import pl.allegro.tech.hermes.api.FilterValidation;
import pl.allegro.tech.hermes.management.domain.message.filtering.FilterValidationException;
import pl.allegro.tech.hermes.api.MessageValidationWrapper;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import tech.allegro.schema.json2avro.converter.AvroConversionException;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

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
                        schema = schemaRepository.getLatestAvroSchema(topic);
                    }
                    bytes = jsonAvroConverter.convertToAvro(wrapper.getMessage().getBytes(), schema.getSchema());
                } catch (AvroConversionException e) {
                    throw new FilterValidationException("Could not convert to avro message. Cause: " + e.getMessage());
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
        final List<MessageFilter> filters;
        try {
            filters = filterSpecifications.stream()
                    .map(messageFilters::compile)
                    .collect(toList());
        } catch (NoSuchFilterException ex) {
            throw new FilterValidationException(ex.getMessage(), ex);
        }

        final FilterResult result = new FilterChain(filters).apply(message);

        if (result.getCause().isPresent()) {
            final Exception exception = result.getCause().get();
            throw new FilterValidationException(exception.getMessage(), exception);
        }

        return new FilterValidation(result.isFiltered());
    }
}
