package pl.allegro.tech.hermes.frontend.publishing.handlers;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.metric.StartedTimersPair;
import pl.allegro.tech.hermes.frontend.metric.TopicWithMetrics;

class ExchangeMetrics implements ExchangeCompletionListener {

    private final StartedTimersPair producerLatencyTimers;
    private final TopicWithMetrics topicWithMetrics;

    ExchangeMetrics(TopicWithMetrics topicWithMetrics) {
        this.topicWithMetrics = topicWithMetrics;
        producerLatencyTimers = topicWithMetrics.startProducerLatencyTimers();
    }

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
        topicWithMetrics.markStatusCodeMeter(exchange.getStatusCode());
        producerLatencyTimers.close();

        nextListener.proceed();
    }
}
