package pl.allegro.tech.hermes.frontend.producer;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.metadata.ProduceMetadata;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

public class BrokerLatencyReporter {

  private static final Logger logger = LoggerFactory.getLogger(BrokerLatencyReporter.class);

  private final boolean perBrokerLatencyEnabled;
  private final MetricsFacade metricsFacade;
  private final Duration slowResponseThreshold;
  private final ExecutorService reporterExecutorService;

  public BrokerLatencyReporter(
      boolean perBrokerLatencyEnabled,
      MetricsFacade metricsFacade,
      Duration slowResponseThreshold,
      ExecutorService reporterExecutorService) {
    this.perBrokerLatencyEnabled = perBrokerLatencyEnabled;
    this.metricsFacade = metricsFacade;
    this.slowResponseThreshold = slowResponseThreshold;
    this.reporterExecutorService = reporterExecutorService;
  }

  public void report(
      HermesTimerContext timerContext,
      Message message,
      Topic.Ack ack,
      Supplier<ProduceMetadata> produceMetadata) {
    Duration duration = timerContext.closeAndGet();
    if (perBrokerLatencyEnabled) {
      try {
        reporterExecutorService.submit(
            () -> doReport(duration, message.getId(), ack, produceMetadata));
      } catch (RejectedExecutionException ignored) {
        // don't propagate the exception - allow metrics to be dropped if executor is overloaded
        // executor service should already be instrumented to meter rejected executions so no action
        // is needed
      }
    }
  }

  private void doReport(
      Duration duration,
      String messageId,
      Topic.Ack ack,
      Supplier<ProduceMetadata> produceMetadata) {
    String broker = produceMetadata.get().getBroker().orElse("unknown");

    if (duration.compareTo(slowResponseThreshold) > 0) {
      logger.debug(
          "Slow produce request, broker response time: {} ms, ackLevel: {}, messageId: {}, broker: {}",
          duration.toMillis(),
          ack,
          messageId,
          broker);
    }

    metricsFacade.broker().recordBrokerLatency(broker, ack, duration);
  }
}
