package pl.allegro.tech.hermes.frontend.publishing.handlers

import io.undertow.server.ExchangeCompletionListener
import io.undertow.server.HttpServerExchange
import pl.allegro.tech.hermes.common.metric.timer.StartedTimersPair
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import spock.lang.Specification
import spock.lang.Unroll

class ExchangeMetricsTest extends Specification {

    @Unroll
    def "should meter status #statusCode and measure Hermes latency"() {
        given:
        CachedTopic cachedTopic = Mock()
        StartedTimersPair timers = Mock()
        ExchangeCompletionListener.NextListener nextListener = Mock()
        HttpServerExchange exchange = new HttpServerExchange(null).setStatusCode(statusCode)

        when:
        ExchangeMetrics metrics = new ExchangeMetrics(cachedTopic)
        metrics.exchangeEvent(exchange, nextListener)

        then:
        1 * cachedTopic.startHermesLatencyTimers() >> timers
        1 * cachedTopic.markRequestMeter()
        1 * cachedTopic.markStatusCodeMeter(statusCode)
        1 * timers.close()
        1 * nextListener.proceed()
        0 * _

        where:
        statusCode << [201, 202, 400, 403, 408, 429, 500, 503]
    }
}
