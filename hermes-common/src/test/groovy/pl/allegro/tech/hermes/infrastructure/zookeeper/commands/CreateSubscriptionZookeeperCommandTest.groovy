package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class CreateSubscriptionZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client.getCuratorFramework())
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def setup() {
        createGroupIfNotExists("group")
        createTopicIfNotExists("topic", "group")
    }

    def "should create subscription"() {
        given:
        def subscription = subscription(new TopicName("group", "topic"),
            "subscription-a").build()

        and:
        def command = commandFactory.createSubscription(subscription)

        when:
        command.execute(client)

        then:
        def path = "/hermes/groups/group/topics/topic/subscriptions/subscription-a"
        wait.untilZookeeperPathIsCreated(path)
        assertions.zookeeperPathContains(path, subscription)
    }

    def "should rollback subscription creation"() {
        given:
        def subscription = subscription(new TopicName("group", "topic"),
            "subscription-b").build()

        and:
        def command = commandFactory.createSubscription(subscription)
        command.backup(client)
        command.execute(client)
        String path = "/hermes/groups/group/topics/topic/subscriptions/subscription-b"
        wait.untilZookeeperPathIsCreated(path)

        when:
        command.rollback(client)

        then:
        wait.untilZookeeperPathNotExists(path)
    }
}
