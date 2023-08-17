package pl.allegro.tech.hermes.management.infrastructure.prometheus.graphite

import pl.allegro.tech.hermes.management.infrastructure.graphite.CachingGraphiteClient
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringMetricsContainer
import pl.allegro.tech.hermes.test.helper.cache.FakeTicker
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration

import static pl.allegro.tech.hermes.api.MetricDecimalValue.of

class CachingPrometheusClientTest extends Specification {
    static final CACHE_TTL_IN_SECONDS = 30
    static final CACHE_SIZE = 100_000
    static final CACHE_TTL = Duration.ofSeconds(CACHE_TTL_IN_SECONDS)

    def underlyingClient = Mock(GraphiteClient)
    def ticker = new FakeTicker()

    @Subject
    def cachingClient = new CachingGraphiteClient(underlyingClient, ticker, CACHE_TTL_IN_SECONDS, CACHE_SIZE)

    def "should return metrics from the underlying client"() {
        given:
        underlyingClient.readMetrics("metric_1", "metric_2") >> new MonitoringMetricsContainer([metric_1: of("1"), metric_2: of("2")])

        when:
        def metrics = cachingClient.readMetrics("metric_1", "metric_2")

        then:
        metrics.metricValue("metric_1") == of("1")
        metrics.metricValue("metric_2") == of("2")
    }

    def "should return metrics from cache while TTL has not expired"() {
        when:
        cachingClient.readMetrics("metric_1", "metric_2")
        ticker.advance(CACHE_TTL.minusSeconds(1))
        cachingClient.readMetrics("metric_1", "metric_2")

        then:
        1 * underlyingClient.readMetrics("metric_1", "metric_2") >> new MonitoringMetricsContainer([metric_1: of("1"), metric_2: of("2")])
    }

    def "should get metrics from the underlying client after TTL expires"() {
        when:
        cachingClient.readMetrics("metric_1", "metric_2")
        ticker.advance(CACHE_TTL.plusSeconds(1))
        cachingClient.readMetrics("metric_1", "metric_2")

        then:
        2 * underlyingClient.readMetrics("metric_1", "metric_2") >> new MonitoringMetricsContainer([metric_1: of("1"), metric_2: of("2")])
    }
}
