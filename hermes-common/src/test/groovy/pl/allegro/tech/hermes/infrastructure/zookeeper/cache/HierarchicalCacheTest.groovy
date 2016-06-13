package pl.allegro.tech.hermes.infrastructure.zookeeper.cache

import com.jayway.awaitility.Duration
import com.jayway.awaitility.groovy.AwaitilityTrait
import pl.allegro.tech.hermes.test.IntegrationTest

import java.util.concurrent.Executors

import static com.jayway.awaitility.Awaitility.await

class HierarchicalCacheTest extends IntegrationTest implements AwaitilityTrait {

    private HierarchicalCache cache = new HierarchicalCache(
            zookeeper(),
            Executors.newSingleThreadExecutor(),
            '/hierarchicalCacheTest',
            3,
            ['groups', 'topics', 'subscriptions']
    )

    private Set calledCallbacks = [] as Set

    void setup() {
        Closure loggingCallback = { e -> calledCallbacks.add(new String(e.data.data)) }
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
        await().atMost(Duration.FIVE_SECONDS).until({ calledCallbacks.contains('groupA') })

        when:
        zookeeper().inTransaction()
                .create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA', 'topicA'.bytes)
                .and().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions')
                .and().commit()

        then:
        await().atMost(Duration.FIVE_SECONDS).until({ calledCallbacks.contains('topicA') })

        when:
        zookeeper().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions/subA', 'subA'.bytes)

        then:
        await().atMost(Duration.FIVE_SECONDS).until({ calledCallbacks.contains('subA') })

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
                .and().create().forPath('/hierarchicalCacheTest/groups/groupB/topics/topicB/subscriptions/subscriptionB', 'subscriptionB'.bytes)
                .and().commit()

        when:
        cache.start()

        then:
        await().atMost(Duration.FIVE_SECONDS).until({
            calledCallbacks.contains('groupB') && calledCallbacks.contains('topicB') && calledCallbacks.contains('subscriptionB')
        })

        cleanup:
        cache.stop()
    }

}
