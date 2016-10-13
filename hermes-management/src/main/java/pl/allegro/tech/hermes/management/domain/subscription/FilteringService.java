package pl.allegro.tech.hermes.management.domain.subscription;

import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.MessageFilterSpecification;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.filtering.MessageFilter;
import pl.allegro.tech.hermes.common.filtering.MessageFilters;
import pl.allegro.tech.hermes.common.filtering.chain.FilterChain;
import pl.allegro.tech.hermes.common.message.MessageContent;
import pl.allegro.tech.hermes.domain.topic.schema.CompiledSchema;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;
import pl.allegro.tech.hermes.management.api.mappers.FilterValidation;
import pl.allegro.tech.hermes.management.api.mappers.MessageValidationWrapper;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static jersey.repackaged.com.google.common.collect.ImmutableMap.of;

@Component
public class FilteringService {
    private final MessageFilters messageFilters;
    private final SchemaRepository schemaRepository;


    @Autowired
    public FilteringService(MessageFilters messageFilters, SchemaRepository schemaRepository) {
        this.messageFilters = messageFilters;
        this.schemaRepository = schemaRepository;
    }

    public FilterValidation isFiltered(MessageValidationWrapper wrapper, Topic topic) {
        final MessageContent message = getMessageContent(wrapper, topic);

        return isFiltered(message, wrapper.getFilterSpecifications());
    }

    private MessageContent getMessageContent(MessageValidationWrapper wrapper, Topic topic) {
        JsonAvroConverter jsonAvroConverter = new JsonAvroConverter();

        CompiledSchema<Schema> schema = null;
        byte[] bytes = null;
        if (topic.getContentType() == ContentType.AVRO) {
            if (wrapper.getSchemaVersion().isPresent()) {
                schema = schemaRepository.getAvroSchema(topic, SchemaVersion.valueOf(wrapper.getSchemaVersion().get()));
            } else {
                schema = schemaRepository.getAvroSchema(topic);
            }
            bytes = jsonAvroConverter.convertToAvro(wrapper.getMessage().getBytes(), schema.getSchema());
        }
        else if(topic.getContentType() == ContentType.JSON){
            bytes = wrapper.getMessage().getBytes();
        }
        else {
//            throw new Exception(); //todo throw specific exception
        }

        return new MessageContent.Builder()
                .withData(bytes)
                .withContentType(topic.getContentType())
                .withSchema(Optional.ofNullable(schema))
                .build();
    }

    private FilterValidation isFiltered(MessageContent message, List<MessageFilterSpecification> filterSpecifications) {
        final List<MessageFilter> filters = filterSpecifications.stream()
                .map(filterSpec -> new MessageFilterSpecification(of(
                        "type", message.getContentType().toString().toLowerCase() + "path",
                        "path", filterSpec.getPath(),
                        "matcher", filterSpec.getMatcher())
                ))
                .map(messageFilters::compile)
                .collect(toList());

        final FilterChain filterChain = new FilterChain(filters);

        return new FilterValidation(filterChain.apply(message).isFiltered());
    }
}
