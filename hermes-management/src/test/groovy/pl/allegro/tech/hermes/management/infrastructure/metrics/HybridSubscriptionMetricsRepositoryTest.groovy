package pl.allegro.tech.hermes.management.infrastructure.metrics

import pl.allegro.tech.hermes.api.SubscriptionMetrics
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetrics
import pl.allegro.tech.hermes.management.stub.MetricsPaths
import spock.lang.Specification

class HybridSubscriptionMetricsRepositoryTest extends Specification {

    private GraphiteClient client = Stub(GraphiteClient)

    private MetricsPaths paths = new MetricsPaths("stats")

    private SharedCounter sharedCounter = Stub(SharedCounter)

    private DistributedEphemeralCounter distributedCounter = Stub(DistributedEphemeralCounter)

    private ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes")

    private SubscriptionLagSource lagSource = new NoOpSubscriptionLagSource()

    private HybridSubscriptionMetricsRepository repository = new HybridSubscriptionMetricsRepository(client, paths,
            sharedCounter, distributedCounter, zookeeperPaths, lagSource)

    def "should read subscription metrics from multiple places"() {
        given:
        String rate = 'sumSeries(stats.consumer.*.meter.group.topic.subscription.m1_rate)'
        String timeouts = 'sumSeries(stats.consumer.*.status.group.topic.subscription.errors.timeout.m1_rate)'
        String otherErrors = 'sumSeries(stats.consumer.*.status.group.topic.subscription.errors.other.m1_rate)'

        client.readMetrics(_ as String, _ as String, _ as String, rate, timeouts, otherErrors, _ as String) >> new GraphiteMetrics()
                .addMetricValue(rate, '10').addMetricValue(timeouts, '100').addMetricValue(otherErrors, '1000')
        sharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/delivered') >> 100
        sharedCounter.getValue('/hermes/groups/group/topics/topic/subscriptions/subscription/metrics/discarded') >> 1
        distributedCounter.getValue('/hermes/consumers', '/groups/group/topics/topic/subscriptions/subscription/metrics/inflight') >> 5

        when:
        SubscriptionMetrics metrics = repository.loadMetrics(new TopicName('group', 'topic'), 'subscription')

        then:
        metrics.rate == '10'
        metrics.delivered == 100
        metrics.discarded == 1
        metrics.inflight == 5
        metrics.timeouts == "100"
        metrics.otherErrors == "1000"
        metrics.lag == -1
    }

    def "should read subscription metrics for all http status codes"() {
        given:
        client.readMetrics(getHttpStatusCodeForFamily(2), getHttpStatusCodeForFamily(4), getHttpStatusCodeForFamily(5),
                _ as String, _ as String, _ as String, _ as String) >> new GraphiteMetrics()
                .addMetricValue(getHttpStatusCodeForFamily(2), '2')
                .addMetricValue(getHttpStatusCodeForFamily(4), '4')
                .addMetricValue(getHttpStatusCodeForFamily(5), '5')

        when:
        SubscriptionMetrics metrics = repository.loadMetrics(new TopicName('group', 'topic'), 'subscription')

        then:
        metrics.codes2xx == '2'
        metrics.codes4xx == '4'
        metrics.codes5xx == '5'
    }

    private static String getHttpStatusCodeForFamily(int family) {
        "sumSeries(stats.consumer.*.status.group.topic.subscription.${family}xx.m1_rate)"
    }
}
