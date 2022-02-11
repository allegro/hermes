package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;

public class BasicMessageContentReaderFactory implements MessageContentReaderFactory {

    private final MessageContentWrapper messageContentWrapper;
    private final KafkaHeaderExtractor kafkaHeaderExtractor;

    public BasicMessageContentReaderFactory(MessageContentWrapper messageContentWrapper, KafkaHeaderExtractor kafkaHeaderExtractor) {
        this.messageContentWrapper = messageContentWrapper;
        this.kafkaHeaderExtractor = kafkaHeaderExtractor;
    }

    @Override
    public MessageContentReader provide(Topic topic) {
        return new BasicMessageContentReader(messageContentWrapper, kafkaHeaderExtractor, topic);
    }
}
