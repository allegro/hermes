package pl.allegro.tech.hermes.test

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.curator.framework.CuratorFramework
import org.junit.ClassRule
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperGroupRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperSubscriptionRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperTopicRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient
import pl.allegro.tech.hermes.infrastructure.zookeeper.commands.ZookeeperCommandFactory
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperResource
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Consumer

import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.*
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

abstract class IntegrationTest extends Specification {

    @ClassRule
    @Shared
    ZookeeperResource zookeeperResource = new ZookeeperResource(42182, true, { starter ->
        starter.curator().create().creatingParentsIfNeeded().forPath("/hermes/groups")
    } as Consumer)

    private ZookeeperClient zookeeperClient = new ZookeeperClient(zookeeperResource.curator(),
        "test-client", "local")

    static final String BASE_ZOOKEEPER_PATH = "/hermes"

    protected ZookeeperPaths paths = new ZookeeperPaths(BASE_ZOOKEEPER_PATH)

    protected RepositoryWaiter wait = new RepositoryWaiter(zookeeperResource.curator(), paths)

    protected ObjectMapper mapper = new ObjectMapper()

    protected ZookeeperGroupRepository groupRepository = new ZookeeperGroupRepository(zookeeper(), mapper, paths)

    protected ZookeeperTopicRepository topicRepository = new ZookeeperTopicRepository(zookeeper(), mapper, paths, groupRepository)

    protected ZookeeperSubscriptionRepository subscriptionRepository = new ZookeeperSubscriptionRepository(zookeeper(), mapper, paths, topicRepository)

    protected KafkaNamesMapper kafkaNamesMapper = new NamespaceKafkaNamesMapper("")

    protected PathsCompiler pathsCompiler = new PathsCompiler("")

    protected ZookeeperCommandFactory commandFactory = new ZookeeperCommandFactory(paths, mapper)

    protected CuratorFramework zookeeper() {
        return zookeeperResource.curator()
    }

    protected ZookeeperClient zookeeperClient() {
        return zookeeperClient
    }

    protected  <T> T getData(ZookeeperClient client, String path, Class<T> contentType) {
        def data = client.getCuratorFramework().getData().forPath(path)
        return mapper.readValue(data, contentType)
    }

    protected def createGroupIfNotExists(String name) {
        if(!zookeeperClient.pathExists("/hermes/groups/$name")) {
            createGroup(name)
        }
    }

    protected def createGroup(String name) {
        def group = group(name).build()
        commandFactory.createGroup(group).execute(zookeeperClient)
    }

    protected Topic createTopicIfNotExists(String name, String group) {
        def topic = topic(group, name).build()
        if(!zookeeperClient.pathExists("/hermes/groups/$group/topics/$name")) {
            commandFactory.createTopic(topic).execute(zookeeperClient)
        }
        return topic
    }

    protected Subscription createSubscriptionIfNotExists(String name, String topic, String group) {
        def subscription = subscription(new TopicName(group, topic), name).build()
        if(!zookeeperClient.pathExists("/hermes/groups/$group/topics/$topic/subscriptions/$name")) {
            commandFactory.createSubscription(subscription).execute(zookeeperClient)
        }
        return subscription
    }
}
