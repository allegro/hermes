package pl.allegro.tech.hermes.management.infrastructure.metrics

import pl.allegro.tech.hermes.api.SubscriptionMetrics
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter
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

    private HybridSubscriptionMetricsRepository repository = new HybridSubscriptionMetricsRepository(client, paths,
            sharedCounter, distributedCounter, zookeeperPaths)

    def "should read subscription metrics from multiple places"() {
        given:
        String rate = 'sumSeries(stats.consumer.*.meter.group.topic.subscription.m1_rate)'
        String timeouts = 'sumSeries(stats.consumer.*.status.group.topic.subscription.errors.timeout.m1_rate)'
        String otherErrors = 'sumSeries(stats.consumer.*.status.group.topic.subscription.errors.other.m1_rate)'

        client.readMetrics(rate) >> new GraphiteMetrics().addMetricValue(rate, '10')
        client.readMetrics(timeouts) >> new GraphiteMetrics().addMetricValue(timeouts, '100')
        client.readMetrics(otherErrors) >> new GraphiteMetrics().addMetricValue(otherErrors, '1000')
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
        metrics.timeoutsM1 == "100"
        metrics.otherErrorsM1 == "1000"
    }

    def "should read subscription metrics for all http status codes"() {
        def codeClasses = ['sumSeries(stats.consumer.*.status.group.topic.subscription.1xx.m1_rate)', 'sumSeries(stats.consumer.*.status.group.topic.subscription.2xx.m1_rate)',
                           'sumSeries(stats.consumer.*.status.group.topic.subscription.3xx.m1_rate)', 'sumSeries(stats.consumer.*.status.group.topic.subscription.4xx.m1_rate)',
                           'sumSeries(stats.consumer.*.status.group.topic.subscription.5xx.m1_rate)']

        given:
        client.readMetrics(codeClasses[0]) >> new GraphiteMetrics().addMetricValue(codeClasses[0], '0')
        client.readMetrics(codeClasses[1]) >> new GraphiteMetrics().addMetricValue(codeClasses[1], '1')
        client.readMetrics(codeClasses[2]) >> new GraphiteMetrics().addMetricValue(codeClasses[2], '2')
        client.readMetrics(codeClasses[3]) >> new GraphiteMetrics().addMetricValue(codeClasses[3], '3')
        client.readMetrics(codeClasses[4]) >> new GraphiteMetrics().addMetricValue(codeClasses[4], '4')

        when:
        SubscriptionMetrics metrics = repository.loadMetrics(new TopicName('group', 'topic'), 'subscription')

        then:
        metrics.httpStatusCodesM1 == new SubscriptionMetrics.HttpStatusCodeMetrics('0', '1', '2', '3', '4')
    }
}
