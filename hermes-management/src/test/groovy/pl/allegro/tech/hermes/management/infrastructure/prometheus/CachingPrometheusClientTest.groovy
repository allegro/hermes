package pl.allegro.tech.hermes.management.infrastructure.prometheus

import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer
import pl.allegro.tech.hermes.test.helper.cache.FakeTicker
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration

import static pl.allegro.tech.hermes.api.MetricDecimalValue.of
import static pl.allegro.tech.hermes.api.MetricDecimalValue.unavailable

class CachingPrometheusClientTest extends Specification {
    static final CACHE_TTL_IN_SECONDS = 30
    static final CACHE_SIZE = 100_000
    static final CACHE_TTL = Duration.ofSeconds(CACHE_TTL_IN_SECONDS)

    def underlyingClient = Mock(PrometheusClient)
    def ticker = new FakeTicker()
    def queries = List.of("query")

    @Subject
    def cachingClient = new CachingPrometheusClient(underlyingClient, ticker, CACHE_TTL_IN_SECONDS, CACHE_SIZE)

    def "should return metrics from the underlying client"() {
        given:
        underlyingClient.readMetrics(queries) >> MonitoringMetricsContainer.initialized(
                ["metric_1": of("1"), "metric_2": of("2")])

        when:
        def metrics = cachingClient.readMetrics(queries)

        then:
        metrics.metricValue("metric_1") == of("1")
        metrics.metricValue("metric_2") == of("2")
    }

    def "should return metrics from cache while TTL has not expired"() {
        when:
        cachingClient.readMetrics(queries)
        ticker.advance(CACHE_TTL.minusSeconds(1))
        cachingClient.readMetrics(queries)

        then:
        1 * underlyingClient.readMetrics(queries) >> MonitoringMetricsContainer.initialized(
                ["metric_1": of("1"), "metric_2": of("2")])
    }

    def "should get metrics from the underlying client after TTL expires"() {
        when:
        cachingClient.readMetrics(queries)
        ticker.advance(CACHE_TTL.plusSeconds(1))
        cachingClient.readMetrics(queries)

        then:
        2 * underlyingClient.readMetrics(queries) >> MonitoringMetricsContainer.initialized(
                ["metric_1": of("1"), "metric_2": of("2")])
    }

    def "should invalidate partially unavailable data and retry fetch on the next client metrics read"() {
        when:
        cachingClient.readMetrics(queries)
        cachingClient.readMetrics(queries)

        then:
        2 * underlyingClient.readMetrics(queries) >> MonitoringMetricsContainer.initialized(
                ["metric_1": unavailable(), "metric_2": of("2")])
    }

    def "should invalidate completely unavailable data and retry fetch on the next client metrics read"() {
        when:
        cachingClient.readMetrics(queries)
        cachingClient.readMetrics(queries)

        then:
        2 * underlyingClient.readMetrics(queries) >> MonitoringMetricsContainer.unavailable()
    }
}
