package pl.allegro.tech.hermes.frontend.producer;

import jakarta.annotation.Nullable;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.producer.kafka.KafkaPartitionLeaderRegistry;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

import java.time.Duration;
import java.util.Optional;

public class BrokerLatencyReporter {

    private static final Logger logger = LoggerFactory.getLogger(BrokerLatencyReporter.class);

    private final boolean perBrokerLatencyReportingEnabled;
    private final MetricsFacade metricsFacade;
    private final KafkaPartitionLeaderRegistry kafkaPartitionLeaderRegistry;
    private final Duration slowResponseThreshold;

    public BrokerLatencyReporter(boolean perBrokerLatencyReportingEnabled,
                                 MetricsFacade metricsFacade,
                                 KafkaPartitionLeaderRegistry kafkaPartitionLeaderRegistry,
                                 Duration slowResponseThreshold) {
        this.perBrokerLatencyReportingEnabled = perBrokerLatencyReportingEnabled;
        this.metricsFacade = metricsFacade;
        this.kafkaPartitionLeaderRegistry = kafkaPartitionLeaderRegistry;
        this.slowResponseThreshold = slowResponseThreshold;
    }

    public void report(Message message, @Nullable RecordMetadata recordMetadata, HermesTimerContext timerContext) {
        Duration duration = timerContext.closeAndGet();
        if (!perBrokerLatencyReportingEnabled) return;

        String broker = Optional.ofNullable(recordMetadata)
                .flatMap(metadata -> kafkaPartitionLeaderRegistry.leaderOf(recordMetadata.topic(), recordMetadata.partition()))
                .orElse("unknown");

        if (duration.compareTo(slowResponseThreshold) > 0) {
            logger.info("Slow broker response, messageId: {}, broker: {}", broker, message);
        }

        metricsFacade.broker().registerBrokerLatency(broker, duration);
    }
}
