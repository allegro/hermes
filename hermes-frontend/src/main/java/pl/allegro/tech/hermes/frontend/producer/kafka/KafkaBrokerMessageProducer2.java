package pl.allegro.tech.hermes.frontend.producer.kafka;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.inject.Singleton;
import net.jodah.failsafe.CircuitBreakerOpenException;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Singleton
public class KafkaBrokerMessageProducer2 implements BrokerMessageProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaBrokerMessageProducer2.class);
    private final Producers producers;
    private final KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher;
    private final MetricsFacade metricsFacade;
    private final MessageToKafkaProducerRecordConverter messageConverter;

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public KafkaBrokerMessageProducer2(Producers producers,
                                       KafkaTopicMetadataFetcher kafkaTopicMetadataFetcher,
                                       MetricsFacade metricsFacade,
                                       MessageToKafkaProducerRecordConverter messageConverter) {
        this.producers = producers;
        this.kafkaTopicMetadataFetcher = kafkaTopicMetadataFetcher;
        this.metricsFacade = metricsFacade;
        this.messageConverter = messageConverter;
//        producers.registerGauges(metricsFacade);

        // https://resilience4j.readme.io/docs/circuitbreaker
        // Create a custom configuration for a CircuitBreaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(5)
                .recordException(e -> INTERNAL_SERVER_ERROR
                        .equals(getResponse().getStatus()))
                .recordExceptions(IOException.class, TimeoutException.class)
                .ignoreExceptions(BusinessException.class, OtherBusinessException.class)
                .build();

// Create a CircuitBreakerRegistry with a custom global configuration

        circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);

//// Get or create a CircuitBreaker from the CircuitBreakerRegistry
//// with the global default configuration
//        CircuitBreaker circuitBreakerWithDefaultConfig =
//                circuitBreakerRegistry.circuitBreaker("name1");
//
//// Get or create a CircuitBreaker from the CircuitBreakerRegistry
//// with a custom configuration
//        CircuitBreaker circuitBreakerWithCustomConfig = circuitBreakerRegistry
//                .circuitBreaker("name2", circuitBreakerConfig);


    }

    // questions
    // 1. use latency increase or timeouts (simpler)

    // 1. if timeouts % greater than X -> switch to remote DC for a topic. HALF_OPEN -> probes local DC
    // 2. reverse of 1). Slowly move traffic to remote DC.
    // 3. Time budget for local. On timeout, fallback to remote.
    // 4. Multi strategy - on large latency pick more aggressive fallback
    public Producer<byte[], byte[]> getProducer(CachedTopic cachedTopic) {

        return producers.get(cachedTopic.getTopic());
    }

    @Override
    public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
        // TODO: report per broker latency metrics


        ProducerRecord<byte[], byte[]> producerRecord =
                messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        try {
//            var producer = getProducer(cachedTopic);

            // circuit breaker to local DC
            var circuitBreaker = circuitBreakerRegistry.circuitBreaker(cachedTopic.getQualifiedName());

            try {
                circuitBreaker.decorateRunnable(() -> {
                    var producer = producers.get(cachedTopic.getTopic());

                    // async
                    producer.send(producerRecord, new SendCallback(message, cachedTopic, callback));
                });

            } catch (CircuitBreakerOpenException open) {
                
            }

            // todo: fallback to remote
        } catch (Exception e) {
            // message didn't get to internal producer buffer and it will not be send to a broker
            callback.onUnpublished(message, cachedTopic.getTopic(), e);
        }
    }

    @Override
    public boolean isTopicAvailable(CachedTopic cachedTopic) {
        String kafkaTopicName = cachedTopic.getKafkaTopics().getPrimary().name().asString();

        try {
            List<PartitionInfo> partitionInfos = producers.get(cachedTopic.getTopic()).partitionsFor(kafkaTopicName);
            if (anyPartitionWithoutLeader(partitionInfos)) {
                logger.warn("Topic {} has partitions without a leader.", kafkaTopicName);
                return false;
            }
            if (anyUnderReplicatedPartition(partitionInfos, kafkaTopicName)) {
                logger.warn("Topic {} has under replicated partitions.", kafkaTopicName);
                return false;
            }
            if (partitionInfos.size() > 0) {
                return true;
            }
        } catch (Exception e) {
            logger.warn("Could not read information about partitions for topic {}. {}", kafkaTopicName, e.getMessage());
            return false;
        }

        logger.warn("No information about partitions for topic {}", kafkaTopicName);
        return false;
    }

    private boolean anyPartitionWithoutLeader(List<PartitionInfo> partitionInfos) {
        return partitionInfos.stream().anyMatch(p -> p.leader() == null);
    }

    private boolean anyUnderReplicatedPartition(List<PartitionInfo> partitionInfos, String kafkaTopicName) throws Exception {
        int minInSyncReplicas = kafkaTopicMetadataFetcher.fetchMinInSyncReplicas(kafkaTopicName);
        return partitionInfos.stream().anyMatch(p -> p.inSyncReplicas().length < minInSyncReplicas);
    }

    private class SendCallback implements org.apache.kafka.clients.producer.Callback {

        private final Message message;
        private final CachedTopic topic;
        private final PublishingCallback callback;

        public SendCallback(Message message, CachedTopic topic, PublishingCallback callback) {
            this.message = message;
            this.topic = topic;
            this.callback = callback;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            if (e == null) {
                callback.onPublished(message, topic.getTopic());
//                producers.maybeRegisterNodeMetricsGauges(metricsFacade);
            } else {
                callback.onUnpublished(message, topic.getTopic(), e);
            }
        }
    }
}
