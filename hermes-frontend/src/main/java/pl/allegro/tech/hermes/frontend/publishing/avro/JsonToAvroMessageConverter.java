package pl.allegro.tech.hermes.frontend.publishing.avro;

import pl.allegro.tech.hermes.common.message.converter.JsonToAvroConverter;
import pl.allegro.tech.hermes.frontend.publishing.Message;

public class JsonToAvroMessageConverter {
    private final JsonToAvroConverter converter = new JsonToAvroConverter();

    public Message convert(Message message, String messageSchema) {
        return new Message(message.getId(), converter.convert(message.getData(), messageSchema), message.getTimestamp());
    }
}
