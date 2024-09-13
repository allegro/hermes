package pl.allegro.tech.hermes.management.infrastructure.metrics

import pl.allegro.tech.hermes.api.TopicMetrics
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusClient
import pl.allegro.tech.hermes.management.infrastructure.prometheus.PrometheusMetricsProvider
import spock.lang.Specification

import static pl.allegro.tech.hermes.api.MetricDecimalValue.of

class HybridPrometheusBasedTopicMetricsRepositoryTest extends Specification {

    private PrometheusClient client = Stub(PrometheusClient)

    private SummedSharedCounter summedSharedCounter = Stub(SummedSharedCounter)

    private ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes")

    private SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)

    private PrometheusMetricsProvider prometheusMetricsProvider = new PrometheusMetricsProvider(client,
            "hermes_consumers", "hermes_frontend", "service='hermes'")

    private HybridTopicMetricsRepository repository = new HybridTopicMetricsRepository(prometheusMetricsProvider,
            summedSharedCounter, zookeeperPaths, subscriptionRepository)

    private String topicRequestsQuery = "sum by (group, topic) (irate({__name__='hermes_frontend_topic_requests_total', group='group', topic='topic', service='hermes'}[1m]))"
    private String topicDeliveredQuery = "sum by (group, topic) (irate({__name__='hermes_consumers_subscription_delivered_total', group='group', topic='topic', service='hermes'}[1m]))"
    private String topicThroughputQuery = "sum by (group, topic) (irate({__name__='hermes_frontend_topic_throughput_bytes_total', group='group', topic='topic', service='hermes'}[1m]))"

    def "should load metrics from graphite and zookeeper"() {
        given:
        List<String> queries = List.of(topicRequestsQuery, topicDeliveredQuery, topicThroughputQuery)
        TopicName topic = new TopicName('group', 'topic')

        client.readMetrics(queries) >> MonitoringMetricsContainer.createEmpty()
            .addMetricValue(topicRequestsQuery, of('10'))
            .addMetricValue(topicDeliveredQuery, of('20'))
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/metrics/published') >> 100
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/metrics/volume') >> 1024
        subscriptionRepository.listSubscriptionNames(topic) >> ["subscription1", "subscription2"]

        when:
        TopicMetrics metrics = repository.loadMetrics(topic)

        then:
        metrics.rate == of('10')
        metrics.deliveryRate == of('20')
        metrics.published == 100
        metrics.subscriptions == 2
        metrics.volume == 1024
    }

}