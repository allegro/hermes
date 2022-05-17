package pl.allegro.tech.hermes.consumers.config;

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
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.MessageContentReaderFactory;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;

@Configuration
public class ConsumerReceiverConfiguration {

    @Bean
    public ReceiverFactory kafkaMessageReceiverFactory(ConfigFactory configs,
                                                       MessageContentReaderFactory messageContentReaderFactory,
                                                       HermesMetrics hermesMetrics,
                                                       OffsetQueue offsetQueue,
                                                       Clock clock,
                                                       KafkaNamesMapper kafkaNamesMapper,
                                                       FilterChainFactory filterChainFactory,
                                                       Trackers trackers,
                                                       ConsumerPartitionAssignmentState consumerPartitionAssignmentState) {
        return new KafkaMessageReceiverFactory(
                configs,
                messageContentReaderFactory,
                hermesMetrics,
                offsetQueue,
                clock,
                kafkaNamesMapper,
                filterChainFactory,
                trackers,
                consumerPartitionAssignmentState
        );
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
