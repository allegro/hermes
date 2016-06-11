package pl.allegro.tech.hermes.infrastructure.zookeeper.cache

import com.jayway.awaitility.Duration
import com.jayway.awaitility.groovy.AwaitilityTrait
import org.junit.ClassRule
import pl.allegro.tech.hermes.test.helper.util.Ports
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperResource
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.Executors

import static com.jayway.awaitility.Awaitility.await

class HierarchicalCacheTest extends Specification implements AwaitilityTrait {

    @Shared
    @ClassRule
    private ZookeeperResource zookeeper = new ZookeeperResource(Ports.nextAvailable(), true, { s ->
        if (s.curator().checkExists().forPath('/hermes/groups') == null) {
            s.curator().create().creatingParentsIfNeeded().forPath('/hermes/groups')
        }
    })

    private HierarchicalCache cache = new HierarchicalCache(
            zookeeper.curator(),
            Executors.newSingleThreadExecutor(),
            '/hierarchicalCacheTest',
            3,
            ['groups', 'topics', 'subscriptions']
    )

    void setup() {
        cache.start()
    }

    void tearDown() {
        cache.stop()
    }

    def "should start cache with selected depth and call callbacks on changes"() {
        given:
        Set calledCallbacks = [] as Set
        Closure loggingCallback = { e -> calledCallbacks.add(new String(e.data.data)) }

        cache.registerCallback(0, loggingCallback)
        cache.registerCallback(1, loggingCallback)
        cache.registerCallback(2, loggingCallback)

        when:
        zookeeper.curator().inTransaction()
                .create().forPath('/hierarchicalCacheTest/groups/groupA', 'groupA'.bytes)
                .and().create().forPath('/hierarchicalCacheTest/groups/groupA/topics')
                .and().commit()

        then:
        await().atMost(Duration.FIVE_SECONDS).until({ calledCallbacks.contains('groupA') })

        when:
        zookeeper.curator().inTransaction()
                .create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA', 'topicA'.bytes)
                .and().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions')
                .and().commit()

        then:
        await().atMost(Duration.FIVE_SECONDS).until({ calledCallbacks.contains('topicA') })

        when:
        zookeeper.curator().create().forPath('/hierarchicalCacheTest/groups/groupA/topics/topicA/subscriptions/subA', 'subA'.bytes)

        then:
        await().atMost(Duration.FIVE_SECONDS).until({ calledCallbacks.contains('subA') })
    }

}
