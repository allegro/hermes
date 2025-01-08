package pl.allegro.tech.hermes.common.metric;

import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_ACTIVE_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_IDLE_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_ACTIVE_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_IDLE_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_CONNECTIONS;
import static pl.allegro.tech.hermes.common.metric.Gauges.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_PENDING_CONNECTIONS;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.ToDoubleFunction;

public class ConsumerSenderMetrics {

  private final MeterRegistry meterRegistry;
  private final GaugeRegistrar gaugeRegistrar;

  ConsumerSenderMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.gaugeRegistrar = new GaugeRegistrar(meterRegistry);
  }

  public <T> void registerRequestQueueSizeGauge(T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge("http-clients.request-queue-size", obj, f);
  }

  public <T> void registerHttp1SerialClientRequestQueueSizeGauge(T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge("http-clients.serial.http1.request-queue-size", obj, f);
  }

  public <T> void registerHttp1BatchClientRequestQueueSizeGauge(T obj, ToDoubleFunction<T> f) {
    meterRegistry.gauge("http-clients.batch.http1.request-queue-size", obj, f);
  }

  public <T> void registerHttp2RequestQueueSizeGauge(T obj, ToDoubleFunction<T> f) {
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
}
