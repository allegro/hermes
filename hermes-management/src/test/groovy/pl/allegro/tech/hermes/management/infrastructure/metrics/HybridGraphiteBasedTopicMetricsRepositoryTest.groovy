package pl.allegro.tech.hermes.management.infrastructure.metrics

import pl.allegro.tech.hermes.api.TopicMetrics
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetricsProvider
import spock.lang.Specification

import static pl.allegro.tech.hermes.api.MetricDecimalValue.of

class HybridGraphiteBasedTopicMetricsRepositoryTest extends Specification {

    private GraphiteClient client = Stub(GraphiteClient)

    private SummedSharedCounter summedSharedCounter = Stub(SummedSharedCounter)

    private ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes")

    private SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)

    private GraphiteMetricsProvider graphiteMetricsProvider = new GraphiteMetricsProvider(client, "stats")

    private HybridTopicMetricsRepository repository = new HybridTopicMetricsRepository(graphiteMetricsProvider,
            summedSharedCounter, zookeeperPaths, subscriptionRepository)

    def "should load metrics from graphite and zookeeper"() {
        given:
        String rate = 'sumSeries(stats.producer.*.meter.group.topic.m1_rate)'
        String deliveryRate = 'sumSeries(stats.consumer.*.meter.group.topic.m1_rate)'
        String throughput = 'sumSeries(stats.producer.*.throughput.group.topic.m1_rate)'
        TopicName topic = new TopicName('group', 'topic')

        client.readMetrics(rate, deliveryRate, throughput) >> MonitoringMetricsContainer.createEmpty()
            .addMetricValue(rate, of('10'))
            .addMetricValue(deliveryRate, of('20'))
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
