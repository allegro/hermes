package pl.allegro.tech.hermes.frontend.publishing.handlers;

import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.metric.timer.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

class ExchangeMetrics implements ExchangeCompletionListener {

  private static final Logger logger = LoggerFactory.getLogger(ExchangeMetrics.class);

  private final CachedTopic cachedTopic;
  private final StartedTimersPair hermesLatencyTimers;

  ExchangeMetrics(CachedTopic cachedTopic) {
    this.cachedTopic = cachedTopic;
    hermesLatencyTimers = cachedTopic.startHermesLatencyTimers();
  }

  @Override
  public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
    try {
      cachedTopic.markRequestMeter();
      cachedTopic.markStatusCodeMeter(exchange.getStatusCode());
      hermesLatencyTimers.close();
    } catch (RuntimeException e) {
      logger
          .atError()
          .addKeyValue(TOPIC_NAME, cachedTopic.getQualifiedName())
          .setCause(e)
          .log("Exception while invoking metrics for topic {}", cachedTopic.getQualifiedName());
    } finally {
      nextListener.proceed();
    }
  }
}
