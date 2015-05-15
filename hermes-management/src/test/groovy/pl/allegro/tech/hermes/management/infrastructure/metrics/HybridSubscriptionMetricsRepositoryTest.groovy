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

        client.readMetrics(rate) >> new GraphiteMetrics().addMetricValue(rate, '10')
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
    }
}
