package pl.allegro.tech.hermes.consumers.consumer.converter;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.converter.schema.AvroSchemaRepositoryMetadataAware;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.consumers.consumer.Message.message;

public class AvroToJsonMessageConverter implements MessageConverter {

    private final AvroSchemaRepositoryMetadataAware schemaRepository;

    @Inject
    public AvroToJsonMessageConverter(AvroSchemaRepositoryMetadataAware schemaRepository) {
        this.schemaRepository = schemaRepository;
    }

    @Override
    public Message convert(Message message, Topic topic) {
        return message()
                .fromMessage(message)
                .withData(AvroToJsonConverter.convert(message.getData(), schemaRepository.getSchemaWithoutMetadata(topic)))
                .build();
    }

}
