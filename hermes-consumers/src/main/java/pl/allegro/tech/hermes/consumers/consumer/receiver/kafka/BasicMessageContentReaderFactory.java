package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;

public class BasicMessageContentReaderFactory implements MessageContentReaderFactory {

    private final CompositeMessageContentWrapper compositeMessageContentWrapper;
    private final KafkaHeaderExtractor kafkaHeaderExtractor;

    public BasicMessageContentReaderFactory(CompositeMessageContentWrapper compositeMessageContentWrapper, KafkaHeaderExtractor kafkaHeaderExtractor) {
        this.compositeMessageContentWrapper = compositeMessageContentWrapper;
        this.kafkaHeaderExtractor = kafkaHeaderExtractor;
    }

    @Override
    public MessageContentReader provide(Topic topic) {
        return new BasicMessageContentReader(compositeMessageContentWrapper, kafkaHeaderExtractor, topic);
    }
}
