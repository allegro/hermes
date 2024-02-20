package pl.allegro.tech.hermes.frontend.producer.kafka;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.inject.Singleton;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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
        CircuitBreakerConfig exceptionOnlyCircuitBreaker = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(5)
                .build();

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(3)
                .minimumNumberOfCalls(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(5)
                .build();

// Create a CircuitBreakerRegistry with a custom global configuration

        circuitBreakerRegistry = CircuitBreakerRegistry.of(exceptionOnlyCircuitBreaker);
    }

    // questions
    // 1. use latency increase or timeouts (simpler)

    // 1. if timeouts % greater than X -> switch to remote DC for a topic. HALF_OPEN -> probes local DC
    // 2. reverse of 1). Slowly move traffic to remote DC.
    // 3. Time budget for local. On timeout, fallback to remote.
    // 4. Multi strategy - on large latency pick more aggressive fallback
    public Optional<Producer<byte[], byte[]>> getRemoteProducer(CachedTopic cachedTopic) {
        // 1. local: healthy, enabled    | remote: healthy, enabled <- standardowy przypadek obsłużony przez circuit breaker
        // 2. local: healthy, disabled   | remote: healthy, enabled <- nie robimy
        // 3. local: unhealthy, enabled  | remote: healthy, enabled <- to samo co 1
        // 4. local: unhealthy, disabled | remote: healthy, enabled <- to samo co 2
        // 5. local: healthy, enabled    | remote: healthy, disable <- done
        // 6. local: healthy, enabled    | remote: unhealthy, enabled <- niemożliwy przy 100% sukcesów
        // 7. local: unhealthy, enabled  | remote: unhealthy, enabled <- jeszcze nie zrobione, w tym przypadku wolimy local

        // zwróć Optional.empty() gdy: disabled lub unhealthy
        return producers.get(cachedTopic.getTopic());
    }


    // If exception rate greater than X, switch to remote DC for a topic
    @Override
    public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
        // TODO: report per broker latency metrics
        var producerRecord = messageConverter.convertToProducerRecord(message, cachedTopic.getKafkaTopics().getPrimary().name());

        Optional<Producer<byte[], byte[]>> remoteProducer = getRemoteProducer(cachedTopic);
        var producer = producers.get(cachedTopic.getTopic());
        HermesTimerContext timer = cachedTopic.startBrokerLatencyTimer();

        try {

            if (remoteProducer.isEmpty()) {
                producer.send(producerRecord, new SendCallback(message, cachedTopic, callback, timer));
            } else {
                var circuitBreaker = circuitBreakerRegistry.circuitBreaker(cachedTopic.getQualifiedName());
                try {
                    circuitBreaker.acquirePermission();
                    producer.send(producerRecord, new CbSendCallback(message, cachedTopic, callback, circuitBreaker, timer));
                } catch (CallNotPermittedException open) {
                    // todo: what if remote DC is disabled or unhealthy?
                    remoteProducer.get().send(producerRecord, new SendCallback(message, cachedTopic, callback, timer));
                } catch (Exception e) {
                    circuitBreaker.releasePermission();
                    throw e;
                }
            }
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
        private final HermesTimerContext hermesTimerContext;

        public SendCallback(Message message, CachedTopic topic, PublishingCallback callback, HermesTimerContext hermesTimerContext) {
            this.message = message;
            this.topic = topic;
            this.callback = callback;
            this.hermesTimerContext = hermesTimerContext;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            hermesTimerContext.close();
            if (e == null) {
                callback.onPublished(message, topic.getTopic());
//                producers.maybeRegisterNodeMetricsGauges(metricsFacade);
            } else {
                callback.onUnpublished(message, topic.getTopic(), e);
            }
        }
    }


    private class CbSendCallback implements org.apache.kafka.clients.producer.Callback {

        private final Message message;
        private final CachedTopic topic;
        private final PublishingCallback callback;
        private final CircuitBreaker circuitBreaker;
        private final HermesTimerContext hermesTimerContext;

        public CbSendCallback(Message message, CachedTopic topic, PublishingCallback callback, CircuitBreaker circuitBreaker, HermesTimerContext hermesTimerContext) {
            this.message = message;
            this.topic = topic;
            this.callback = callback;
            this.circuitBreaker = circuitBreaker;
            this.hermesTimerContext = hermesTimerContext;
        }

        @Override
        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
            var duration = hermesTimerContext.closeAndGet();
            if (e == null) {
                circuitBreaker.onSuccess(duration.toMillis(), TimeUnit.MILLISECONDS);
                callback.onPublished(message, topic.getTopic());
//                producers.maybeRegisterNodeMetricsGauges(metricsFacade);
            } else {
                circuitBreaker.onError(duration.toMillis(), TimeUnit.MILLISECONDS, e);
                callback.onUnpublished(message, topic.getTopic(), e);
            }
        }
    }
}
