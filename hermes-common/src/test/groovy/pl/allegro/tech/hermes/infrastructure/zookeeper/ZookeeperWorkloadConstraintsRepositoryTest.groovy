package pl.allegro.tech.hermes.infrastructure.zookeeper

import com.fasterxml.jackson.databind.ObjectMapper
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.api.Constraints
import pl.allegro.tech.hermes.test.IntegrationTest

import java.util.concurrent.TimeUnit

import static com.jayway.awaitility.Awaitility.await

class ZookeeperWorkloadConstraintsRepositoryTest extends IntegrationTest {

    ZookeeperWorkloadConstraintsRepository repository
    ZookeeperWorkloadConstraintsCache cache
    def paths = new ZookeeperPaths("/hermes")

    def setup() {
        try {
            deleteAllNodes("/hermes/consumers-workload-constraints")
        } catch (Exception e) {
            e.printStackTrace()
        }

        cache = new ZookeeperWorkloadConstraintsCache(zookeeper(), paths)
        repository = new ZookeeperWorkloadConstraintsRepository(zookeeper(), new ObjectMapper(), paths, cache)
    }

    def cleanup() {
        cache.close()
    }

    def "should return constraints from cache"() {
        given:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)

        when:
        def constraints = repository.getConsumersWorkloadConstraints()

        then:
        constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic')).consumersNumber == 1
        constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub')).consumersNumber == 3
    }

    private def ensureCacheWasUpdated(int expectedSize) {
        await()
                .atMost(200, TimeUnit.MILLISECONDS)
                .until { cache.getCurrentData().size() == expectedSize }
    }
}
