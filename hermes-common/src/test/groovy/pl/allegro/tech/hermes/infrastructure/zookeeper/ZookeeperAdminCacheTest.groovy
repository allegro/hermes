package pl.allegro.tech.hermes.infrastructure.zookeeper

import org.apache.curator.framework.recipes.cache.ChildData
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.admin.AdminTool
import pl.allegro.tech.hermes.common.admin.zookeeper.ZookeeperAdminCache
import pl.allegro.tech.hermes.test.IntegrationTest

class ZookeeperAdminCacheTest extends IntegrationTest {

    def adminCache = new ZookeeperAdminCache(zookeeper(), mapper)
    def subscriptionBytes =  mapper.writeValueAsBytes(new SubscriptionName("test", new TopicName("group", "name")));

    def "should cleanup RETRANSMIT marker after retransmission"() {
        given:
        def path = zookeeper().create().forPath("/123_$AdminTool.Operations.RETRANSMIT")
        def event = new PathChildrenCacheEvent(PathChildrenCacheEvent.Type.CHILD_ADDED,
                                                new ChildData(path, null, subscriptionBytes))
        when:
        adminCache.childEvent(zookeeper(), event)

        then:
        !zookeeper().checkExists().forPath(path)
    }

}
