package pl.allegro.tech.hermes.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.curator.framework.CuratorFramework
import org.junit.ClassRule
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperResource
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Consumer

abstract class IntegrationTest extends Specification {

    @ClassRule
    @Shared
    ZookeeperResource zookeeperResource = new ZookeeperResource(42182, true, { starter ->
        starter.curator().create().creatingParentsIfNeeded().forPath("/hermes/groups")
    } as Consumer)

    static final String BASE_ZOOKEEPER_PATH = "/hermes"

    protected ZookeeperPaths paths = new ZookeeperPaths(BASE_ZOOKEEPER_PATH)

    protected RepositoryWaiter wait = new RepositoryWaiter(zookeeperResource.curator(), paths)

    protected ObjectMapper mapper = new ObjectMapper()

    protected ZookeeperGroupRepository groupRepository = new ZookeeperGroupRepository(zookeeper(), mapper, paths)

    protected ZookeeperTopicRepository topicRepository = new ZookeeperTopicRepository(zookeeper(), mapper, paths, groupRepository)

    protected ZookeeperSubscriptionRepository subscriptionRepository = new ZookeeperSubscriptionRepository(zookeeper(), mapper, paths, topicRepository)

    protected KafkaNamesMapper kafkaNamesMapper = new NamespaceKafkaNamesMapper("")

    protected PathsCompiler pathsCompiler = new PathsCompiler("")

    protected CuratorFramework zookeeper() {
        return zookeeperResource.curator()
    }
}
