package pl.allegro.tech.hermes.infrastructure.zookeeper

import org.apache.curator.framework.recipes.cache.ChildData
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent
import org.apache.zookeeper.data.Stat
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.admin.AdminOperationsCallback
import pl.allegro.tech.hermes.common.admin.AdminTool
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache
import pl.allegro.tech.hermes.test.IntegrationTest

import java.time.Clock
import java.time.ZoneId

import static java.time.Instant.ofEpochMilli
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_UPDATED

class ZookeeperAdminCacheTest extends IntegrationTest {
    def subscriptionBytes =  mapper.writeValueAsBytes(new SubscriptionName("test", new TopicName("group", "name")));

    def "should run retransmit callback when event is younger than node"() {
        given:
        def clock = Clock.fixed(ofEpochMilli(1000), ZoneId.systemDefault());
        def adminCache = new ZookeeperAdminCache(paths, zookeeper(), mapper, clock);
        def path = "/1_$AdminTool.Operations.RETRANSMIT"
        boolean executed = false

        adminCache.addCallback([
                onRetransmissionStarts: { SubscriptionName subscription ->
                    executed = true;
                },
                restartConsumer       : { SubscriptionName subscription ->  }
        ] as AdminOperationsCallback)

        expect:
        adminCache.childEvent(zookeeper(), new PathChildrenCacheEvent(type, new ChildData(path, getStat(mtime), subscriptionBytes)))
        executed == result

        where:
        type          | mtime | result
        CHILD_ADDED   |  1600 | true
        CHILD_ADDED   |     1 | false
        CHILD_UPDATED |  1600 | true
        CHILD_UPDATED |     1 | false
    }

    Stat getStat(int mtime) {
        new Stat(1, 1, 1, mtime, 1, 1 , 1, 1, 1, 1, 1)
    }
}
