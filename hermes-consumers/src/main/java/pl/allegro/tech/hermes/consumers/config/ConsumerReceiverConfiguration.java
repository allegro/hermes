package pl.allegro.tech.hermes.consumers.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.BasicMessageContentReaderFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaHeaderExtractor;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageConverterFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.MessageContentReaderFactory;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;

@Configuration
public class ConsumerReceiverConfiguration {

    @Bean
    public ReceiverFactory kafkaMessageReceiverFactory(ConfigFactory configs,
                                                       KafkaMessageConverterFactory messageConverterFactory,
                                                       HermesMetrics hermesMetrics,
                                                       OffsetQueue offsetQueue,
                                                       KafkaNamesMapper kafkaNamesMapper,
                                                       FilterChainFactory filterChainFactory,
                                                       Trackers trackers,
                                                       ConsumerPartitionAssignmentState consumerPartitionAssignmentState) {
        return new KafkaMessageReceiverFactory(
                configs,
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
    public KafkaMessageConverterFactory kafkaMessageConverterFactory(MessageContentReaderFactory messageContentReaderFactory,
                                                                     Clock clock) {
        return new KafkaMessageConverterFactory(messageContentReaderFactory, clock);
    }

    @Bean
    public MessageContentReaderFactory messageContentReaderFactory(MessageContentWrapper messageContentWrapper,
                                                                   KafkaHeaderExtractor kafkaHeaderExtractor) {
        return new BasicMessageContentReaderFactory(messageContentWrapper, kafkaHeaderExtractor);
    }

    @Bean
    public KafkaHeaderExtractor kafkaHeaderExtractor(ConfigFactory configFactory) {
        return new KafkaHeaderExtractor(configFactory);
    }
}
