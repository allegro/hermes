package pl.allegro.tech.hermes.management.domain.consistency

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.domain.group.GroupNotExistsException
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException
import pl.allegro.tech.hermes.domain.topic.TopicRepository
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths
import pl.allegro.tech.hermes.management.config.ConsistencyCheckerProperties
import pl.allegro.tech.hermes.management.config.storage.DefaultZookeeperGroupRepositoryFactory
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor
import pl.allegro.tech.hermes.management.domain.group.commands.CreateGroupRepositoryCommand
import pl.allegro.tech.hermes.management.domain.mode.ModeService
import pl.allegro.tech.hermes.management.domain.subscription.commands.CreateSubscriptionRepositoryCommand
import pl.allegro.tech.hermes.management.domain.topic.commands.CreateTopicRepositoryCommand
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClient
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperClientManager
import pl.allegro.tech.hermes.management.infrastructure.zookeeper.ZookeeperRepositoryManager
import pl.allegro.tech.hermes.management.utils.MultiZookeeperIntegrationTest
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder

import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.groupWithRandomName
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscriptionWithRandomName
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic

class StorageSyncSpec extends MultiZookeeperIntegrationTest {

    static GROUPS_PATH = '/hermes/groups'
    ZookeeperClientManager manager
    ZookeeperRepositoryManager repositoryManager

    ModeService modeService
    MultiDatacenterRepositoryCommandExecutor executor
    MetricsFacade metricsFacade = new MetricsFacade(new SimpleMeterRegistry())

    def objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
    def paths = new ZookeeperPaths('/hermes')

    def setup() {
        manager = buildZookeeperClientManager()
        manager.start()
        assertZookeeperClientsConnected(manager.clients)
        manager.clients.each { client -> setupZookeeperPath(client, GROUPS_PATH) }
        repositoryManager = new ZookeeperRepositoryManager(
                manager, new TestDatacenterNameProvider(DC_1_NAME), objectMapper,
                paths, new DefaultZookeeperGroupRepositoryFactory())
        repositoryManager.start()

        modeService = new ModeService()
        executor = new MultiDatacenterRepositoryCommandExecutor(repositoryManager, true, modeService)
    }

    def cleanup() {
        manager.stop()
    }

    def "should create group in all DCs if primary datacenter contains group"() {
        given:
        def (Group group, String groupPath) = setupGroup()

        and:
        removeNode(DC_1_NAME, groupPath)

        and:
        assertNodeDoesNotExist(DC_1_NAME, groupPath)
        assertNodeContains(DC_2_NAME, groupPath, group)

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncGroup(group.groupName, DC_2_NAME)

        then:
        assertNodesContains(groupPath, group)
    }

    def "should delete group in all DCs if primary datacenter does not contain group"() {
        given:
        def (Group group, String groupPath) = setupGroup()

        and:
        removeNode(DC_1_NAME, groupPath)

        and:
        assertNodeDoesNotExist(DC_1_NAME, groupPath)
        assertNodeContains(DC_2_NAME, groupPath, group)

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncGroup(group.groupName, DC_1_NAME)

        then:
        assertNodesDoNotExist(groupPath)
    }

    def "should create topic in all DCs if primary datacenter contains topic"() {
        given:
        def (Group _, Topic topic, String topicPath) = setupTopic()

        and:
        removeNode(DC_1_NAME, topicPath)

        and:
        assertNodeDoesNotExist(DC_1_NAME, topicPath)
        assertNodeContains(DC_2_NAME, topicPath, topic)

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncTopic(topic.getName(), DC_2_NAME)

        then:
        assertNodesContains(topicPath, topic)
    }

    def "should delete topic in all DCs if primary datacenter contains topic"() {
        given:
        def (Group _, Topic topic, String topicPath) = setupTopic()

        and:
        removeNode(DC_1_NAME, topicPath)

        and:
        assertNodeDoesNotExist(DC_1_NAME, topicPath)
        assertNodeContains(DC_2_NAME, topicPath, topic)

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncTopic(topic.getName(), DC_1_NAME)

        then:
        assertNodesDoNotExist(topicPath)
    }

    def "should sync topic in all DCs to primary datacenter value"() {
        given:
        def (Group _, Topic topic, String topicPath) = setupTopic()

        and:
        TopicRepository topicRepository = repositoryManager.getLocalRepository(TopicRepository.class).getRepository()
        Topic updatedTopic = TopicBuilder.topic(topic.name.getGroupName(), topic.name.getName())
                .withDescription("foo bar").build()
        topicRepository.updateTopic(updatedTopic)

        and:
        assertNodesAreDifferent(topicPath, Topic.class)

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncTopic(topic.getName(), DC_1_NAME)

        then:
        assertNodesContains(topicPath, updatedTopic)
    }

    def "should create subscription in all DCs if primary datacenter contains subscription"() {
        given:
        def (Group _, Topic __, Subscription subscription, String subscriptionPath) = setupSubscription()

        and:
        removeNode(DC_1_NAME, subscriptionPath)

        and:
        assertNodeDoesNotExist(DC_1_NAME, subscriptionPath)
        assertNodeContains(DC_2_NAME, subscriptionPath, subscription)

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncSubscription(subscription.getQualifiedName(), DC_2_NAME)

        then:
        assertNodesContains(subscriptionPath, subscription)
    }

    def "should delete subscription in all DCs if primary datacenter does not contain subscription"() {
        given:
        def (Group _, Topic __, Subscription subscription, String subscriptionPath) = setupSubscription()

        and:
        SubscriptionRepository subscriptionRepository = repositoryManager.getLocalRepository(SubscriptionRepository.class).getRepository()
        Subscription updatedSubscription = SubscriptionBuilder.subscription(subscription.getQualifiedName())
                .withDescription("foo bar").build()
        subscriptionRepository.updateSubscription(updatedSubscription)

        and:
        assertNodesAreDifferent(subscriptionPath, Subscription.class)

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncSubscription(subscription.getQualifiedName(), DC_1_NAME)

        then:
        assertNodesContains(subscriptionPath, updatedSubscription)
    }

    def "should sync subscription in all DCs to primary datacenter value"() {
        given:
        def (Group _, Topic __, Subscription subscription, String subscriptionPath) = setupSubscription()

        and:
        removeNode(DC_1_NAME, subscriptionPath)

        and:
        assertNodeDoesNotExist(DC_1_NAME, subscriptionPath)
        assertNodeContains(DC_2_NAME, subscriptionPath, subscription)

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncSubscription(subscription.getQualifiedName(), DC_1_NAME)

        then:
        assertNodesDoNotExist(subscriptionPath)
    }

    def "topic sync should fail if group does not exist in replica datacenter"() {
        given:
        def (Group group, Topic topic, String _) = setupTopic()

        and:
        removeNode(DC_1_NAME, paths.groupPath(group.getGroupName()))

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncTopic(topic.getName(), DC_2_NAME)

        then:
        thrown GroupNotExistsException
    }

    def "subscription sync should fail if group does not exist in replica datacenter"() {
        given:
        def (Group group, Topic _, Subscription subscription, String __) = setupSubscription()

        and:
        removeNode(DC_1_NAME, paths.groupPath(group.getGroupName()))

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncSubscription(subscription.getQualifiedName(), DC_2_NAME)

        then:
        thrown TopicNotExistsException
    }

    def "subscription sync should fail if topic does not exist in replica datacenter"() {
        given:
        def (Group _, Topic topic, Subscription subscription, String __) = setupSubscription()

        and:
        removeNode(DC_1_NAME, paths.topicPath(topic.getName()))

        and:
        def consistencyService = new DcConsistencyService(repositoryManager, objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        consistencyService.syncSubscription(subscription.getQualifiedName(), DC_2_NAME)

        then:
        thrown TopicNotExistsException
    }

    private def setupGroup() {
        Group group = groupWithRandomName().build()
        executor.execute(new CreateGroupRepositoryCommand(group))
        String groupPath = paths.groupPath(group.groupName)
        return [group, groupPath]
    }

    private def setupTopic() {
        Group group = groupWithRandomName().build()
        Topic topic = randomTopic(group.getGroupName(), "").build()
        executor.execute(new CreateGroupRepositoryCommand(group))
        executor.execute(new CreateTopicRepositoryCommand(topic))
        String topicPath = paths.topicPath(topic.getName())
        return [group, topic, topicPath]
    }

    private def setupSubscription() {
        Group group = groupWithRandomName().build()
        Topic topic = randomTopic(group.getGroupName(), "").build()
        Subscription subscription = subscriptionWithRandomName(topic.getName()).build()
        executor.execute(new CreateGroupRepositoryCommand(group))
        executor.execute(new CreateTopicRepositoryCommand(topic))
        executor.execute(new CreateSubscriptionRepositoryCommand(subscription))
        String subscriptionPath = paths.subscriptionPath(subscription)
        return [group, topic, subscription, subscriptionPath]
    }


    private <T> void assertNodeContains(String dc, String path, T expected) {
        def zkClient = findClientByDc(manager.clients, dc)
        def data = zkClient.curatorFramework.getData().forPath(path)
        def actual = objectMapper.readValue(data, expected.class)
        assert actual == expected
    }

    private <T> void assertNodesContains(String path, T expected) {
        manager.clients.each {
            def data = it.curatorFramework.getData().forPath(path)
            def actual = objectMapper.readValue(data, expected.class)
            assert actual == expected
        }
    }

    private def assertNodeDoesNotExist(String dc, String path) {
        def zkClient = findClientByDc(manager.clients, dc)
        zkClient.curatorFramework.checkExists().forPath(path) == null
    }

    private def assertNodesDoNotExist(String path) {
        manager.clients.each {
            assert it.curatorFramework.checkExists().forPath(path) == null
        }
    }

    private <T> void assertNodesAreDifferent(String path, Class<T> clazz) {
        Set<T> values = new HashSet<>()
        manager.clients.each {
            def data = it.curatorFramework.getData().forPath(path)
            def value = objectMapper.readValue(data, clazz)
            values.add(value)
        }
        assert values.size() > 1
    }

    private def removeNode(String dc, String path) {
        def zkClient = findClientByDc(manager.clients, dc)
        zkClient.curatorFramework.delete()
                .deletingChildrenIfNeeded()
                .forPath(path)
    }

    private static setupZookeeperPath(ZookeeperClient zookeeperClient, String path) {
        def healthCheckPathExists = zookeeperClient.curatorFramework
                .checkExists()
                .forPath(path) != null
        if (!healthCheckPathExists) {
            zookeeperClient.curatorFramework
                    .create()
                    .creatingParentContainersIfNeeded()
                    .forPath(path)
        }
    }
}
