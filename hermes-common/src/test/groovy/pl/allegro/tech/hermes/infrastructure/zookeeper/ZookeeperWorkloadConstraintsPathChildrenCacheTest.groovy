package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.Constraints
import pl.allegro.tech.hermes.test.IntegrationTest

import java.util.concurrent.TimeUnit

import static com.jayway.awaitility.Awaitility.await

class ZookeeperWorkloadConstraintsPathChildrenCacheTest extends IntegrationTest {

    ZookeeperWorkloadConstraintsPathChildrenCache pathChildrenCache
    def curator = zookeeper()

    def setup() {
        try {
            deleteAllNodes("/hermes/consumers-workload-constraints")
        } catch (Exception e) {
            e.printStackTrace()
        }

        pathChildrenCache = new ZookeeperWorkloadConstraintsPathChildrenCache(curator, "/hermes/consumers-workload-constraints")
        pathChildrenCache.start()
    }

    def cleanup() {
        pathChildrenCache.close()
    }

    def "should return empty list if base path does not exist"() {
        expect:
        pathChildrenCache.getChildrenData().empty
    }

    def "should return list of child data"() {
        given:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)

        when:
        def childrenData = pathChildrenCache.getChildrenData()
        def constraints = childrenData
                .collect { it.data }
                .collect { objectMapper.readValue(it, Constraints) }

        then:
        childrenData.size() == 2
        constraints as Set == [new Constraints(1), new Constraints(3)] as Set
    }

    def "should update cache on create node"() {
        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        ensureCacheWasUpdated(1)
        def childrenData = pathChildrenCache.getChildrenData()

        then:
        childrenData.size() == 1

        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)
        def updatedChildrenData = pathChildrenCache.getChildrenData()

        then:
        updatedChildrenData.size() == 2
    }

    def "should update cache on delete node"() {
        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)
        def childrenData = pathChildrenCache.getChildrenData()

        then:
        childrenData.size() == 2

        when:
        deleteData('/hermes/consumers-workload-constraints/group.topic')
        ensureCacheWasUpdated(1)
        def updatedChildrenData = pathChildrenCache.getChildrenData()

        then:
        updatedChildrenData.size() == 1
    }

    def "should update cache on change node"() {
        given:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)

        when:
        def childrenData = pathChildrenCache.getChildrenData()
        def constraints = childrenData
                .collect { it.data }
                .collect { objectMapper.readValue(it, Constraints) }

        then:
        childrenData.size() == 2
        constraints as Set == [new Constraints(1), new Constraints(3)] as Set

        when:
        updateNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(2))
        ensureCacheWasUpdated(2)

        def updatedChildrenData = pathChildrenCache.getChildrenData()
        def updatedConstraints = updatedChildrenData
                .collect { it.data }
                .collect { objectMapper.readValue(it, Constraints) }

        then:
        updatedChildrenData.size() == 2
        updatedConstraints as Set == [new Constraints(2), new Constraints(3)] as Set
    }

    private def ensureCacheWasUpdated(int expectedSize) {
        await()
                .atMost(200, TimeUnit.MILLISECONDS)
                .until { pathChildrenCache.getChildrenData().size() == expectedSize }
    }
}

