package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
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
public class ConsumerReceiverConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "consumer.receiver")
    public ConsumerReceiverProperties consumerReceiverProperties() {
        return new ConsumerReceiverProperties();
    }

    @Bean
    public ReceiverFactory kafkaMessageReceiverFactory(ConfigFactory configs,
                                                       ConsumerReceiverProperties consumerReceiverProperties,
                                                       KafkaConsumerRecordToMessageConverterFactory messageConverterFactory,
                                                       HermesMetrics hermesMetrics,
                                                       OffsetQueue offsetQueue,
                                                       KafkaNamesMapper kafkaNamesMapper,
                                                       FilterChainFactory filterChainFactory,
                                                       Trackers trackers,
                                                       ConsumerPartitionAssignmentState consumerPartitionAssignmentState) {
        return new KafkaMessageReceiverFactory(
                configs,
                consumerReceiverProperties.toKafkaReceiverParams(),
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
