package pl.allegro.tech.hermes.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.curator.framework.CuratorFramework
import org.junit.ClassRule
import pl.allegro.tech.hermes.common.kafka.JsonToAvroKafkaNamesMapper
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperResource
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Consumer

abstract class IntegrationTest extends Specification {

    @ClassRule
    @Shared
    ZookeeperResource zookeeperResource = new ZookeeperResource(42182, true, { starter ->
        starter.curator().create().forPath("/hermes")
        starter.curator().create().forPath("/hermes/groups")
    } as Consumer)

    protected ZookeeperPaths paths = new ZookeeperPaths("/hermes")

    protected RepositoryWaiter wait = new RepositoryWaiter(zookeeperResource.curator(), paths)

    protected ObjectMapper mapper = new ObjectMapper()

    protected ZookeeperGroupRepository groupRepository = new ZookeeperGroupRepository(zookeeper(), mapper, paths)

    protected ZookeeperTopicRepository topicRepository = new ZookeeperTopicRepository(zookeeper(), mapper, paths, groupRepository)

    protected ZookeeperSubscriptionRepository subscriptionRepository = new ZookeeperSubscriptionRepository(zookeeper(), mapper, paths, topicRepository)

    protected JsonToAvroKafkaNamesMapper kafkaNamesMapper = new JsonToAvroKafkaNamesMapper("")

    protected CuratorFramework zookeeper() {
        return zookeeperResource.curator()
    }
}
