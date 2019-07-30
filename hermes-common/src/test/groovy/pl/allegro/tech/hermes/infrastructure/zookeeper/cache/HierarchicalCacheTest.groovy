package pl.allegro.tech.hermes.infrastructure.zookeeper.cache

import com.jayway.awaitility.Duration
import com.jayway.awaitility.groovy.AwaitilityTrait
import pl.allegro.tech.hermes.test.IntegrationTest

import java.util.concurrent.Executors

import static com.jayway.awaitility.Awaitility.await
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED

class HierarchicalCacheTest extends IntegrationTest implements AwaitilityTrait {

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
        zookeeper().inTransaction()
                .create().forPath('/hierarchicalCacheTest/groups/groupA', 'groupA'.bytes)
                .and().create().forPath('/hierarchicalCacheTest/groups/groupA/topics')
                .and().commit()

        then:
        await().atMost(Duration.FIVE_SECONDS).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupA', 'groupA'))
        })

        when:
        zookeeper().inTransaction()
                .create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA', 'topicA'.bytes)
                .and().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions')
                .and().commit()

        then:
        await().atMost(Duration.FIVE_SECONDS).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupA/topics/topicA', 'topicA'))
        })

        when:
        zookeeper().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions/subA', 'subA'.bytes)

        then:
        await().atMost(Duration.FIVE_SECONDS).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions/subA', 'subA'))
        })

        cleanup:
        cache.stop()
    }

    def "should call callbacks for all entities created before cache started"() {
        given:
        zookeeper().inTransaction()
                .create().forPath('/hierarchicalCacheTest/groups/groupB', 'groupB'.bytes)
                .and().create().forPath('/hierarchicalCacheTest/groups/groupB/topics')
                .and().create().forPath('/hierarchicalCacheTest/groups/groupB/topics/topicB', 'topicB'.bytes)
                .and().create().forPath('/hierarchicalCacheTest/groups/groupB/topics/topicB/subscriptions')
                .and().create().forPath('/hierarchicalCacheTest/groups/groupB/topics/topicB/subscriptions/subB', 'subB'.bytes)
                .and().commit()

        when:
        cache.start()

        then:
        await().atMost(Duration.FIVE_SECONDS).until({
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
        await().atMost(Duration.FIVE_SECONDS).until({
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
        await().atMost(Duration.FIVE_SECONDS).until({
            calledCallbacks.contains(new Tuple(CHILD_ADDED, '/hierarchicalCacheTest/workload/runtime/topic$subscription', '')) &&
                    !calledCallbacks.contains(new Tuple(CHILD_REMOVED, '/hierarchicalCacheTest/workload/runtime/topic$subscription', ''))
        })

        cleanup:
        cache.stop()
    }

}
