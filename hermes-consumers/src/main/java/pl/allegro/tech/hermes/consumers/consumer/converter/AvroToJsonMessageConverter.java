package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.common.avro.JsonAvroConverter;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.converter.schema.AvroSchemaRepositoryMetadataAware;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

public class AvroToJsonMessageConverter implements MessageConverter {

    private final AvroSchemaRepositoryMetadataAware schemaRepository;
    private final JsonAvroConverter converter;

    @Inject
    public AvroToJsonMessageConverter(AvroSchemaRepositoryMetadataAware schemaRepository) {
        this.schemaRepository = schemaRepository;
        this.converter = new JsonAvroConverter();
    }

    @Override
    public Message convert(Message message, Topic topic) {
        return message()
                .fromMessage(message)
                .withContentType(ContentType.JSON)
                .withData(converter.convertToJson(message.getData(), schemaRepository.getSchemaWithoutMetadata(topic)))
                .build();
    }

}
