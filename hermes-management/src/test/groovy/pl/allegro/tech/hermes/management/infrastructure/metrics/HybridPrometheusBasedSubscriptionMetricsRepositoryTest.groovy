package pl.allegro.tech.hermes.management.infrastructure.metrics


import pl.allegro.tech.hermes.api.MetricLongValue
import pl.allegro.tech.hermes.api.PersistentSubscriptionMetrics
import pl.allegro.tech.hermes.api.SubscriptionMetrics
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusMetricsProvider
import spock.lang.Specification

import static pl.allegro.tech.hermes.api.MetricDecimalValue.of
import static pl.allegro.tech.hermes.api.MetricHistogramValue.ofBuckets

class HybridPrometheusBasedSubscriptionMetricsRepositoryTest extends Specification {

    private PrometheusClient client = Stub(PrometheusClient)

    private SummedSharedCounter summedSharedCounter = Stub(SummedSharedCounter)

    private ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes")

    private SubscriptionLagSource lagSource = new NoOpSubscriptionLagSource()

    private PrometheusMetricsProvider prometheusMetricsProvider = new PrometheusMetricsProvider(
            client, "hermes_consumers", "hermes_frontend", "service=~'hermes'")

    private HybridSubscriptionMetricsRepository repository = new HybridSubscriptionMetricsRepository(prometheusMetricsProvider,
            summedSharedCounter, zookeeperPaths, lagSource)

    private final static String subscriptionQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_%s_total', group='group', topic='topic', subscription='subscription', service=~'hermes'}[1m]))"
    private final static String subscriptionHistogramQuery = "sum by (group, topic, subscription, le) (irate({__name__='hermes_consumers_subscription_%s_seconds_bucket', group='group', topic='topic', subscription='subscription', service=~'hermes'}[1m]))"
    private final static String deliveredQuery = String.format(subscriptionQuery, "delivered")
    private final static String timeoutsQuery = String.format(subscriptionQuery, "timeouts")
    private final static String retriesQuery = String.format(subscriptionQuery, "retries")
    private final static String throughputQuery = String.format(subscriptionQuery, "throughput_bytes")
    private final static String otherErrorsQuery = String.format(subscriptionQuery, "other_errors")
    private final static String batchesQuery = String.format(subscriptionQuery, "batches")
    private final static String messageProcessingTimeQuery = String.format(subscriptionHistogramQuery, "message_processing_time")
    // these queries are different as they contains additional status code filters
    private final static String status2xxQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_http_status_codes_total', group='group', topic='topic', subscription='subscription', status_code=~'2.*', service=~'hermes'}[1m]))"
    private final static String status4xxQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_http_status_codes_total', group='group', topic='topic', subscription='subscription', status_code=~'4.*', service=~'hermes'}[1m]))"
    private final static String status5xxQuery = "sum by (group, topic, subscription) (irate({__name__='hermes_consumers_subscription_http_status_codes_total', group='group', topic='topic', subscription='subscription', status_code=~'5.*', service=~'hermes'}[1m]))"

    private static final List<String> queries = List.of(
            deliveredQuery, timeoutsQuery, retriesQuery, throughputQuery, otherErrorsQuery, batchesQuery,
            status2xxQuery, status4xxQuery, status5xxQuery, messageProcessingTimeQuery
    )

    def "should read subscription metrics from multiple places"() {
        given:
        client.readMetrics(queries) >> MonitoringMetricsContainer.createEmpty()
                .addMetricValue(deliveredQuery, of('10'))
                .addMetricValue(timeoutsQuery, of('100'))
                .addMetricValue(retriesQuery, of('20'))
                .addMetricValue(otherErrorsQuery, of('1000'))
                .addMetricValue(messageProcessingTimeQuery, ofBuckets('+Inf', '3', '60.0', '2'))
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/delivered') >> 100
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/discarded') >> 1
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/volume') >> 16

        when:
        SubscriptionMetrics metrics = repository.loadMetrics(
                new TopicName('group', 'topic'), 'subscription')

        then:
        metrics.rate == of('10')
        metrics.delivered == 100
        metrics.discarded == 1
        metrics.volume == 16
        metrics.timeouts == of("100")
        metrics.retries == of("20")
        metrics.otherErrors == of("1000")
        metrics.lag == MetricLongValue.of(-1)
        metrics.messageProcessingTime == ofBuckets('+Inf', '3', '60.0', '2')
    }

    def "should read subscription metrics for all http status codes"() {
        given:
        client.readMetrics(queries) >> MonitoringMetricsContainer.createEmpty()
                .addMetricValue(status2xxQuery, of('2'))
                .addMetricValue(status4xxQuery, of('4'))
                .addMetricValue(status5xxQuery, of('5'))

        when:
        SubscriptionMetrics metrics = repository.loadMetrics(new TopicName('group', 'topic'), 'subscription')

        then:
        metrics.codes2xx == of('2')
        metrics.codes4xx == of('4')
        metrics.codes5xx == of('5')
    }

    def "should read subscription zookeeper metrics"() {
        given:
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/delivered') >> 1000
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/discarded') >> 10
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/volume') >> 16

        when:
        PersistentSubscriptionMetrics zookeeperMetrics = repository.loadZookeeperMetrics(new TopicName('group', 'topic'), 'subscription')

        then:
        zookeeperMetrics.delivered == 1000
        zookeeperMetrics.discarded == 10
        zookeeperMetrics.volume == 16
    }
}
