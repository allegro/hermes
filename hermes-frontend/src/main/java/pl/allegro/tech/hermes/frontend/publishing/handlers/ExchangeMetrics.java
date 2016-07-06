package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.metric.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

class ExchangeMetrics implements ExchangeCompletionListener {

    private final StartedTimersPair producerLatencyTimers;
    private final CachedTopic cachedTopic;

    ExchangeMetrics(CachedTopic cachedTopic) {
        this.cachedTopic = cachedTopic;
        producerLatencyTimers = cachedTopic.startProducerLatencyTimers();
    }

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
        cachedTopic.markStatusCodeMeter(exchange.getStatusCode());
        producerLatencyTimers.close();

        nextListener.proceed();
    }
}
