package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.timer.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

class ExchangeMetrics implements ExchangeCompletionListener {

  private static final Logger logger = LoggerFactory.getLogger(ExchangeMetrics.class);

  private final StartedTimersPair producerLatencyTimers;
  private final CachedTopic cachedTopic;

  ExchangeMetrics(CachedTopic cachedTopic) {
    this.cachedTopic = cachedTopic;
    producerLatencyTimers = cachedTopic.startProducerLatencyTimers();
  }

  @Override
  public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
    try {
      cachedTopic.markRequestMeter();
      cachedTopic.markStatusCodeMeter(exchange.getStatusCode());
      producerLatencyTimers.close();
    } catch (RuntimeException e) {
      logger.error(
          "Exception while invoking metrics for topic {}", cachedTopic.getQualifiedName(), e);
    } finally {
      nextListener.proceed();
    }
  }
}
