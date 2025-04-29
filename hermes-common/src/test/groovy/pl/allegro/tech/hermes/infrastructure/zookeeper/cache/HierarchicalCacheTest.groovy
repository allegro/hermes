package pl.allegro.tech.hermes.infrastructure.zookeeper.cache

import pl.allegro.tech.hermes.test.IntegrationTest

import java.time.Duration
import java.util.concurrent.Executors

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED
import static org.awaitility.Awaitility.await

class HierarchicalCacheTest extends IntegrationTest {

    private HierarchicalCache cache = new HierarchicalCache(
            zookeeper(),
            Executors.newSingleThreadExecutor(),
            '/hierarchicalCacheTest',
            3,
            ['groups', 'topics', 'subscriptions'],
            true
    )

    private Set calledCallbacks = [] as Set

    private Closure loggingCallback = { e ->
        calledCallbacks.add(new Tuple(e.getType(), e.data.path, new String(e.data.data)))
    }

    void setupSpec() {
        zookeeper().create().creatingParentsIfNeeded().forPath('/hierarchicalCacheTest/groups')
    }

    void setup() {
        cache.registerCallback(0, loggingCallback)
        cache.registerCallback(1, loggingCallback)
        cache.registerCallback(2, loggingCallback)
    }

    def "should start cache with selected depth and call callbacks on changes"() {
        given:
        cache.start()

        when:
        zookeeper().transaction().forOperations(
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupA', 'groupA'.bytes),
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupA/topics')
        )

        then:
        await().atMost(Duration.ofSeconds(5)).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupA', 'groupA'))
        })

        when:
        zookeeper().transaction().forOperations(
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA', 'topicA'.bytes),
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions')
        )

        then:
        await().atMost(Duration.ofSeconds(5)).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupA/topics/topicA', 'topicA'))
        })

        when:
        zookeeper().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions/subA', 'subA'.bytes)

        then:
        await().atMost(Duration.ofSeconds(5)).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions/subA', 'subA'))
        })

        cleanup:
        cache.stop()
    }

    def "should call callbacks for all entities created before cache started"() {
        given:
        zookeeper().transaction().forOperations(
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupB', 'groupB'.bytes),
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupB/topics'),
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupB/topics/topicB', 'topicB'.bytes),
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupB/topics/topicB/subscriptions'),
                zookeeper().transactionOp().create().forPath('/hierarchicalCacheTest/groups/groupB/topics/topicB/subscriptions/subB', 'subB'.bytes)
        )

        when:
        cache.start()

        then:
        await().atMost(Duration.ofSeconds(5)).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupB', 'groupB')) &&
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupB/topics/topicB', 'topicB')) &&
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupB/topics/topicB/subscriptions/subB', 'subB'))
        })

        cleanup:
        cache.stop()
    }

    def "should remove empty topic with children when empty nodes remove enabled"() {
        given:
        zookeeper().create().creatingParentsIfNeeded().forPath('/hierarchicalCacheTest/groups/groupC', '123'.bytes)

        cache.start()

        when:
        zookeeper().create().creatingParentsIfNeeded().forPath(
                '/hierarchicalCacheTest/groups/groupC/topics/topicC/metrics/published', '123'.bytes)

        then:
        await().atMost(Duration.ofSeconds(5)).until({
            calledCallbacks.contains(
                    new Tuple(CHILD_REMOVED, '/hierarchicalCacheTest/groups/groupC/topics/topicC', ''))
        })

        cleanup:
        cache.stop()
    }

    def "should not remove empty topic when empty nodes remove disabled"() {
        given:
        def removeEmptyNodes = false
        HierarchicalCache cache = new HierarchicalCache(
                zookeeper(),
                Executors.newSingleThreadExecutor(),
                '/hierarchicalCacheTest/workload',
                2,
                [],
                removeEmptyNodes)

        cache.registerCallback(0, loggingCallback)
        cache.registerCallback(1, loggingCallback)

        zookeeper().create().creatingParentsIfNeeded().forPath(
                '/hierarchicalCacheTest/workload/runtime', '127.0.0.1'.bytes)

        cache.start()

        when:
        zookeeper().create().creatingParentsIfNeeded().forPath(
                '/hierarchicalCacheTest/workload/runtime/topic$subscription/hs-consumer1', 'AUTO_ASSIGNED'.bytes)

        then:
        await().atMost(Duration.ofSeconds(5)).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/workload/runtime/topic$subscription', '')) &&
                    !calledCallbacks.contains(new Tuple(CHILD_REMOVED, '/hierarchicalCacheTest/workload/runtime/topic$subscription', ''))
        })

        cleanup:
        cache.stop()
    }

}
