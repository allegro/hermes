package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.MeterRegistry;
import pl.allegro.tech.hermes.metrics.HermesTimer;

import java.util.function.ToDoubleFunction;

import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_ACTIVE_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_IDLE_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_ACTIVE_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_IDLE_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_PENDING_CONNECTIONS;

public class ConsumerSenderMetrics {

    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;
    private final GaugeRegistrar gaugeRegistrar;

    ConsumerSenderMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
        this.gaugeRegistrar = new GaugeRegistrar(meterRegistry, hermesMetrics);
    }

    public <T> void registerRequestQueueSizeGauge(T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerConsumerSenderRequestQueueSize(() -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("http-clients.request-queue-size", obj, f);
    }

    public <T> void registerHttp1SerialClientRequestQueueSizeGauge(T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerConsumerSenderHttp1SerialClientRequestQueueSize(() -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("http-clients.serial.http1.request-queue-size", obj, f);
    }

    public <T> void registerHttp1BatchClientRequestQueueSizeGauge(T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerConsumerSenderHttp1BatchClientRequestQueueSize(() -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("http-clients.batch.http1.request-queue-size", obj, f);
    }

    public <T> void registerHttp2RequestQueueSizeGauge(T obj, ToDoubleFunction<T> f) {
        hermesMetrics.registerConsumerSenderHttp2RequestQueueSize(() -> (int) f.applyAsDouble(obj));
        meterRegistry.gauge("http-clients.serial.http2.request-queue-size", obj, f);
    }

    public <T> void registerHttp1SerialClientActiveConnectionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_ACTIVE_CONNECTIONS, obj, f);
    }

    public <T> void registerHttp1SerialClientIdleConnectionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_IDLE_CONNECTIONS, obj, f);
    }

    public <T> void registerHttp1BatchClientActiveConnectionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_ACTIVE_CONNECTIONS, obj, f);
    }

    public <T> void registerHttp1BatchClientIdleConnectionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_IDLE_CONNECTIONS, obj, f);
    }

    public <T> void registerHttp2SerialClientConnectionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_CONNECTIONS, obj, f);
    }

    public <T> void registerHttp2SerialClientPendingConnectionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_PENDING_CONNECTIONS, obj, f);
    }

    public HermesTimer http1SerialClientRequestQueueWaitingTimer() {
        return HermesTimer.from(
                meterRegistry.timer(Timers.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_REQUEST_QUEUE_WAITING_TIME),
                hermesMetrics.timer(Timers.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_REQUEST_QUEUE_WAITING_TIME)
        );
    }

    public HermesTimer http1BatchClientRequestQueueWaitingTimer() {
        return HermesTimer.from(
                meterRegistry.timer(Timers.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_REQUEST_QUEUE_WAITING_TIME),
                hermesMetrics.timer(Timers.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_REQUEST_QUEUE_WAITING_TIME)
        );
    }

    public HermesTimer http2SerialClientRequestQueueWaitingTimer() {
        return HermesTimer.from(
                meterRegistry.timer(Timers.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_REQUEST_QUEUE_WAITING_TIME),
                hermesMetrics.timer(Timers.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_REQUEST_QUEUE_WAITING_TIME)
        );
    }

    public HermesTimer http1SerialClientRequestProcessingTimer() {
        return HermesTimer.from(
                meterRegistry.timer(Timers.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_REQUEST_PROCESSING_TIME),
                hermesMetrics.timer(Timers.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_REQUEST_PROCESSING_TIME)
        );
    }

    public HermesTimer http1BatchClientRequestProcessingTimer() {
        return HermesTimer.from(
                meterRegistry.timer(Timers.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_REQUEST_PROCESSING_TIME),
                hermesMetrics.timer(Timers.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_REQUEST_PROCESSING_TIME)
        );
    }

    public HermesTimer http2SerialClientRequestProcessingTimer() {
        return HermesTimer.from(
                meterRegistry.timer(Timers.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_REQUEST_PROCESSING_TIME),
                hermesMetrics.timer(Timers.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_REQUEST_PROCESSING_TIME)
        );
    }
}
