package pl.allegro.tech.hermes.consumers.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.kafka.HTTPHeadersPropagationAsKafkaHeadersProperties;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState;
import pl.allegro.tech.hermes.consumers.consumer.receiver.ReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.BasicMessageContentReaderFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaConsumerRecordToMessageConverterFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaHeaderExtractor;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaMessageReceiverFactory;
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.MessageContentReaderFactory;
import pl.allegro.tech.hermes.domain.filtering.chain.FilterChainFactory;
import pl.allegro.tech.hermes.infrastructure.dc.DatacenterNameProvider;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

@Configuration
@EnableConfigurationProperties({
  ConsumerReceiverProperties.class,
  KafkaConsumerProperties.class,
  KafkaClustersProperties.class,
  CommonConsumerProperties.class,
  KafkaHeaderNameProperties.class,
  ConsumerHTTPHeadersPropagationAsKafkaHeadersProperties.class
})
public class ConsumerReceiverConfiguration {

  @Bean
  public ReceiverFactory kafkaMessageReceiverFactory(
      CommonConsumerProperties commonConsumerProperties,
      ConsumerReceiverProperties consumerReceiverProperties,
      KafkaConsumerProperties kafkaConsumerProperties,
      KafkaClustersProperties kafkaClustersProperties,
      KafkaConsumerRecordToMessageConverterFactory messageConverterFactory,
      MetricsFacade metrics,
      KafkaNamesMapper kafkaNamesMapper,
      FilterChainFactory filterChainFactory,
      Trackers trackers,
      ConsumerPartitionAssignmentState consumerPartitionAssignmentState,
      DatacenterNameProvider datacenterNameProvider) {
    KafkaProperties kafkaProperties =
        kafkaClustersProperties.toKafkaProperties(datacenterNameProvider);

    return new KafkaMessageReceiverFactory(
        commonConsumerProperties,
        consumerReceiverProperties,
        kafkaConsumerProperties,
        kafkaProperties,
        messageConverterFactory,
        metrics,
        kafkaNamesMapper,
        filterChainFactory,
        trackers,
        consumerPartitionAssignmentState);
  }

  @Bean
  public KafkaConsumerRecordToMessageConverterFactory kafkaMessageConverterFactory(
      MessageContentReaderFactory messageContentReaderFactory,
      KafkaHeaderExtractor kafkaHeaderExtractor,
      Clock clock) {
    return new KafkaConsumerRecordToMessageConverterFactory(
        messageContentReaderFactory, kafkaHeaderExtractor, clock);
  }

  @Bean
  public MessageContentReaderFactory messageContentReaderFactory(
      CompositeMessageContentWrapper compositeMessageContentWrapper,
      KafkaHeaderExtractor kafkaHeaderExtractor,
      SchemaRepository schemaRepository) {
    return new BasicMessageContentReaderFactory(
        compositeMessageContentWrapper, kafkaHeaderExtractor, schemaRepository);
  }

  @Bean
  public KafkaHeaderExtractor kafkaHeaderExtractor(
      KafkaHeaderNameProperties kafkaHeaderNameProperties,
      HTTPHeadersPropagationAsKafkaHeadersProperties
          httpHeadersPropagationAsKafkaHeadersProperties) {
    return new KafkaHeaderExtractor(
        kafkaHeaderNameProperties, httpHeadersPropagationAsKafkaHeadersProperties);
  }
}
