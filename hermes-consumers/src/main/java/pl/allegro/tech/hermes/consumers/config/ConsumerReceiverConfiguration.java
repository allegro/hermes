package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.BasicMessageContentReaderFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaHeaderExtractor;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaConsumerRecordToMessageConverterFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.MessageContentReaderFactory;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({
        ConsumerReceiverProperties.class,
        KafkaConsumerProperties.class,
        KafkaProperties.class,
        CommonConsumerProperties.class
})
public class ConsumerReceiverConfiguration {

    @Bean
    public ReceiverFactory kafkaMessageReceiverFactory(CommonConsumerProperties commonConsumerProperties,
                                                       ConsumerReceiverProperties consumerReceiverProperties,
                                                       KafkaConsumerProperties kafkaConsumerProperties,
                                                       KafkaProperties kafkaAuthorizationProperties,
                                                       KafkaConsumerRecordToMessageConverterFactory messageConverterFactory,
                                                       HermesMetrics hermesMetrics,
                                                       OffsetQueue offsetQueue,
                                                       KafkaNamesMapper kafkaNamesMapper,
                                                       FilterChainFactory filterChainFactory,
                                                       Trackers trackers,
                                                       ConsumerPartitionAssignmentState consumerPartitionAssignmentState) {
        return new KafkaMessageReceiverFactory(
                commonConsumerProperties.toCommonConsumerParameters(),
                consumerReceiverProperties.toKafkaReceiverParams(),
                kafkaConsumerProperties.toKafkaConsumerParameters(),
                kafkaAuthorizationProperties.toKafkaAuthorizationParameters(),
                messageConverterFactory,
                hermesMetrics,
                offsetQueue,
                kafkaNamesMapper,
                filterChainFactory,
                trackers,
                consumerPartitionAssignmentState
        );
    }

    @Bean
    public KafkaConsumerRecordToMessageConverterFactory kafkaMessageConverterFactory(MessageContentReaderFactory messageContentReaderFactory,
                                                                                     Clock clock) {
        return new KafkaConsumerRecordToMessageConverterFactory(messageContentReaderFactory, clock);
    }

    @Bean
    public MessageContentReaderFactory messageContentReaderFactory(CompositeMessageContentWrapper compositeMessageContentWrapper,
                                                                   KafkaHeaderExtractor kafkaHeaderExtractor) {
        return new BasicMessageContentReaderFactory(compositeMessageContentWrapper, kafkaHeaderExtractor);
    }

    @Bean
    public KafkaHeaderExtractor kafkaHeaderExtractor(ConfigFactory configFactory) {
        return new KafkaHeaderExtractor(configFactory);
    }
}
