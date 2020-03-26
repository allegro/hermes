package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;

import javax.inject.Inject;

public class HermesMessageContentReaderFactory implements MessageContentReaderFactory {

    private final MessageContentWrapper messageContentWrapper;
    private final KafkaHeaderExtractor kafkaHeaderExtractor;

    @Inject
    public HermesMessageContentReaderFactory(MessageContentWrapper messageContentWrapper, KafkaHeaderExtractor kafkaHeaderExtractor) {
        this.messageContentWrapper = messageContentWrapper;
        this.kafkaHeaderExtractor = kafkaHeaderExtractor;
    }

    @Override
    public MessageContentReader provide(Topic topic) {
        return new HermesMessageContentReader(messageContentWrapper, kafkaHeaderExtractor, topic);
    }
}
