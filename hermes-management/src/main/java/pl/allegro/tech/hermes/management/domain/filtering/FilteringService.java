package pl.allegro.tech.hermes.management.domain.filtering;

import org.apache.avro.Schema;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationInput;
import pl.allegro.tech.hermes.api.MessageFiltersVerificationResult;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChain;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterResult;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import tech.allegro.schema.json2avro.converter.AvroConversionException;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.MessageFiltersVerificationResult.VerificationStatus.ERROR;
import static pl.allegro.tech.hermes.api.MessageFiltersVerificationResult.VerificationStatus.MATCHED;
import static pl.allegro.tech.hermes.api.MessageFiltersVerificationResult.VerificationStatus.NOT_MATCHED;

@Component
public class FilteringService {
    private final FilterChainFactory filterChainFactory;
    private final SchemaRepository schemaRepository;
    private final TopicService topicService;
    private final JsonAvroConverter jsonAvroConverter;

    public FilteringService(FilterChainFactory filterChainFactory,
                            SchemaRepository schemaRepository,
                            TopicService topicService,
                            JsonAvroConverter jsonAvroConverter) {
        this.filterChainFactory = filterChainFactory;
        this.schemaRepository = schemaRepository;
        this.topicService = topicService;
        this.jsonAvroConverter = jsonAvroConverter;
    }

    public MessageFiltersVerificationResult verify(MessageFiltersVerificationInput verification, TopicName topicName) {
        Topic topic = topicService.getTopicDetails(topicName);
        CompiledSchema<Schema> avroSchema = getLatestAvroSchemaIfExists(topic);

        byte[] messageContent;
        try {
            messageContent = getBytes(verification.getMessage(), topic, avroSchema);
        } catch (AvroConversionException e) {
            return new MessageFiltersVerificationResult(ERROR, createErrorMessage(e));
        }

        MessageForFiltersVerification message = new MessageForFiltersVerification(messageContent, topic.getContentType(), avroSchema);
        FilterChain filterChain = filterChainFactory.create(verification.getFilters());
        FilterResult result = filterChain.apply(message);
        return toMessageFiltersVerificationResult(result);
    }

    private CompiledSchema<Schema> getLatestAvroSchemaIfExists(Topic topic) {
        if (AVRO.equals(topic.getContentType())) {
            return schemaRepository.getLatestAvroSchema(topic);
        }
        return null;
    }

    private byte[] getBytes(byte[] message, Topic topic, CompiledSchema<Schema> avroSchema) {
        switch (topic.getContentType()) {
            case JSON:
                return message;
            case AVRO:
                return jsonAvroConverter.convertToAvro(message, avroSchema.getSchema());
            default:
                throw new IllegalArgumentException();
        }
    }

    private MessageFiltersVerificationResult toMessageFiltersVerificationResult(FilterResult result) {
        return new MessageFiltersVerificationResult(
                result.isFiltered() ? NOT_MATCHED : MATCHED,
                result.getCause()
                        .map(this::createErrorMessage)
                        .orElse(null)
        );
    }

    private String createErrorMessage(Throwable th) {
        Throwable rootCause = ExceptionUtils.getRootCause(th);
        return Stream.of(th.getMessage(), (rootCause != null ? rootCause.getMessage() : null))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }
}
