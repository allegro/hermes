package pl.allegro.tech.hermes.management.infrastructure.metrics

import pl.allegro.tech.hermes.api.MetricLongValue
import pl.allegro.tech.hermes.api.SubscriptionMetrics
import pl.allegro.tech.hermes.api.PersistentSubscriptionMetrics
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetrics
import pl.allegro.tech.hermes.management.stub.MetricsPaths
import spock.lang.Specification

import static pl.allegro.tech.hermes.api.MetricDecimalValue.of

class HybridSubscriptionMetricsRepositoryTest extends Specification {

    private GraphiteClient client = Stub(GraphiteClient)

    private MetricsPaths paths = new MetricsPaths("stats")

    private SummedSharedCounter summedSharedCounter = Stub(SummedSharedCounter)

    private ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes")

    private SubscriptionLagSource lagSource = new NoOpSubscriptionLagSource()

    private HybridSubscriptionMetricsRepository repository = new HybridSubscriptionMetricsRepository(client, paths,
            summedSharedCounter, zookeeperPaths, lagSource)

    def "should read subscription metrics from multiple places"() {
        given:
        String rate = 'sumSeries(stats.consumer.*.meter.group.topic.subscription.m1_rate)'
        String timeouts = 'sumSeries(stats.consumer.*.status.group.topic.subscription.errors.timeout.m1_rate)'
        String otherErrors = 'sumSeries(stats.consumer.*.status.group.topic.subscription.errors.other.m1_rate)'

        client.readMetrics(_ as String, _ as String, _ as String, rate, _ as String, timeouts, otherErrors, _ as String) >> new GraphiteMetrics()
                .addMetricValue(rate, of('10'))
                .addMetricValue(timeouts, of('100'))
                .addMetricValue(otherErrors, of('1000'))
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/delivered') >> 100
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/discarded') >> 1
        summedSharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/volume') >> 16

        when:
        SubscriptionMetrics metrics = repository.loadMetrics(new TopicName('group', 'topic'), 'subscription')

        then:
        metrics.rate == of('10')
        metrics.delivered == 100
        metrics.discarded == 1
        metrics.volume == 16
        metrics.timeouts == of("100")
        metrics.otherErrors == of("1000")
        metrics.lag == MetricLongValue.of(-1)
    }

    def "should read subscription metrics for all http status codes"() {
        given:
        client.readMetrics(getHttpStatusCodeForFamily(2), getHttpStatusCodeForFamily(4), getHttpStatusCodeForFamily(5),
                _ as String, _ as String, _ as String, _ as String, _ as String) >> new GraphiteMetrics()
                .addMetricValue(getHttpStatusCodeForFamily(2), of('2'))
                .addMetricValue(getHttpStatusCodeForFamily(4), of('4'))
                .addMetricValue(getHttpStatusCodeForFamily(5), of('5'))

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

    private static String getHttpStatusCodeForFamily(int family) {
        "sumSeries(stats.consumer.*.status.group.topic.subscription.${family}xx.m1_rate)"
    }
}
