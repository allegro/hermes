package pl.allegro.tech.hermes.consumers.test;


import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public final class MessageBuilder {

    private final String content;

    private MessageBuilder(String content) {
        this.content = content;
    }

    public static MessageBuilder message(String content) {
        return new MessageBuilder(content);
    }

    public Message build() {
        return new Message("213", "whatever", content.getBytes(), 123L, 123L, new PartitionOffset(new KafkaTopic("kafka_topic"), 123, 1));
    }
}
