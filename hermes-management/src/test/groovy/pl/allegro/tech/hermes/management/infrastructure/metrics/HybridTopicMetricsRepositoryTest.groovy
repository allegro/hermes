package pl.allegro.tech.hermes.management.infrastructure.metrics

import pl.allegro.tech.hermes.api.TopicMetrics
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetrics
import pl.allegro.tech.hermes.management.stub.MetricsPaths
import spock.lang.Specification

class HybridTopicMetricsRepositoryTest extends Specification {
    
    private GraphiteClient client = Stub(GraphiteClient)
    
    private MetricsPaths paths = new MetricsPaths("stats")
    
    private SharedCounter sharedCounter = Stub(SharedCounter)
    
    private ZookeeperPaths zookeeperPaths = new ZookeeperPaths("/hermes")
    
    private HybridTopicMetricsRepository repository = new HybridTopicMetricsRepository(client, paths, sharedCounter, zookeeperPaths)
    
    def "should load metrics from graphite and zookeeper"() {
        given:
        String rate = 'sumSeries(stats.producer.*.meter.group.topic.m1_rate)'
        String deliveryRate = 'sumSeries(stats.consumer.*.meter.group.topic.m1_rate)'
        
        client.readMetrics(rate, deliveryRate) >> new GraphiteMetrics()
            .addMetricValue(rate, '10')
            .addMetricValue(deliveryRate, '20')
        sharedCounter.getValue('/hermes/groups/group/topics/topic/metrics/published') >> 100
        
        when:
        TopicMetrics metrics = repository.loadMetrics(new TopicName('group', 'topic'))
        
        then:
        metrics.rate == '10'
        metrics.deliveryRate == '20'
        metrics.published == 100
    }
    
}
