package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import java.util.function.ToDoubleFunction;

import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_COMPRESSION_RATE;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_FAILED_BATCHES_TOTAL;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_METADATA_AGE;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_RECORD_QUEUE_TIME_MAX;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_COMPRESSION_RATE;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_FAILED_BATCHES_TOTAL;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_METADATA_AGE;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_RECORD_QUEUE_TIME_MAX;
import static pl.allegro.tech.hermes.common.metric.Gauges.INFLIGHT_REQUESTS;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

public class ProducerMetrics {
    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;
    private final HermesGauge hermesGauge;

    public ProducerMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
        this.hermesGauge = new HermesGauge(meterRegistry, hermesMetrics);
    }

    public <T> void registerAckAllTotalBytesGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_ALL_BUFFER_TOTAL_BYTES, stateObj, f);
    }

    public <T> void registerAckLeaderTotalBytesGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_LEADER_BUFFER_TOTAL_BYTES, stateObj, f);
    }

    public <T> void registerAckAllAvailableBytesGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_ALL_BUFFER_AVAILABLE_BYTES, stateObj, f);
    }

    public <T> void registerAckLeaderAvailableBytesGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_LEADER_BUFFER_AVAILABLE_BYTES, stateObj, f);
    }

    public <T> void registerAckAllCompressionRateGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_ALL_COMPRESSION_RATE, stateObj, f);
    }

    public <T> void registerAckLeaderCompressionRateGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_LEADER_COMPRESSION_RATE, stateObj, f);
    }

    public <T> void registerAckAllFailedBatchesGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_ALL_FAILED_BATCHES_TOTAL, stateObj, f);
    }

    public <T> void registerAckLeaderFailedBatchesGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_LEADER_FAILED_BATCHES_TOTAL, stateObj, f);
    }

    public <T> void registerAckAllMetadataAgeGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_ALL_METADATA_AGE, stateObj, f);
    }

    public <T> void registerAckLeaderMetadataAgeGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_LEADER_METADATA_AGE, stateObj, f);
    }

    public <T> void registerAckAllRecordQueueTimeMaxGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_ALL_RECORD_QUEUE_TIME_MAX, stateObj, f);
    }

    public <T> void registerAckLeaderRecordQueueTimeMaxGauge(T stateObj, ToDoubleFunction<T> f) {
        hermesGauge.registerGauge(ACK_LEADER_RECORD_QUEUE_TIME_MAX, stateObj, f);
    }

    public double getBufferTotalBytes() {
        return meterRegistry.get(ACK_ALL_BUFFER_TOTAL_BYTES).gauge().value()
                + meterRegistry.get(ACK_LEADER_BUFFER_TOTAL_BYTES).gauge().value();
    }

    public double getBufferAvailableBytes() {
        return meterRegistry.get(ACK_ALL_BUFFER_AVAILABLE_BYTES).gauge().value()
                + meterRegistry.get(ACK_LEADER_BUFFER_AVAILABLE_BYTES).gauge().value();
    }

    public <T> void registerProducerInflightRequestGauge(T stateObj, ToDoubleFunction<T> f) {
        meterRegistry.gauge(INFLIGHT_REQUESTS, stateObj, f);
        hermesMetrics.registerProducerInflightRequest(() -> (int) f.applyAsDouble(stateObj));
    }

    public <T> void registerAckAllMaxLatencyBrokerGauge(T stateObj, ToDoubleFunction<T> f, String brokerNodeId) {
        registerLatencyPerBrokerGauge(stateObj, f, "request-latency-max", ACK_ALL, brokerNodeId);
    }

    public <T> void registerAckLeaderMaxLatencyPerBrokerGauge(T stateObj, ToDoubleFunction<T> f, String brokerNodeId) {
        registerLatencyPerBrokerGauge(stateObj, f, "request-latency-max", ACK_LEADER, brokerNodeId);
    }

    public <T >void registerAckAllAvgLatencyPerBrokerGauge(T stateObj, ToDoubleFunction<T> f, String brokerNodeId) {
        registerLatencyPerBrokerGauge(stateObj, f, "request-latency-avg", ACK_ALL, brokerNodeId);
    }

    public <T> void registerAckLeaderAvgLatencyPerBrokerGauge(T stateObj, ToDoubleFunction<T> f, String brokerNodeId) {
        registerLatencyPerBrokerGauge(stateObj, f,"request-latency-avg", ACK_LEADER, brokerNodeId);
    }

    private <T> void registerLatencyPerBrokerGauge(T stateObj,
                                                   ToDoubleFunction<T> f,
                                                   String metricName,
                                                   String producerName,
                                                   String brokerNodeId) {
        String baseMetricName = Gauges.KAFKA_PRODUCER + producerName +  metricName;
        String graphiteMetricName = baseMetricName + "." + escapeDots(brokerNodeId);

        hermesGauge.registerGauge(
                graphiteMetricName, baseMetricName, stateObj, f, Tags.of("broker", brokerNodeId)
        );
    }
}
