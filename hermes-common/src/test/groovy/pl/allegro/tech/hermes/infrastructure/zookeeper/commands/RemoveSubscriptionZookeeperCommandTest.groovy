package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions


class RemoveSubscriptionZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def path = "/hermes/groups/group/topics/topic/subscriptions/subscription"

    Subscription subscription

    def setup() {
        createGroupIfNotExists("group")
        createTopicIfNotExists("topic", "group")
        subscription = createSubscriptionIfNotExists("subscription", "topic", "group")
    }

    def "should remove subscription"() {
        given:
        def command = commandFactory.removeSubscription(new TopicName("group", "topic"),
            "subscription")

        when:
        command.execute(client)

        then:
        wait.untilZookeeperPathNotExists(path)
    }

    def "should rollback subscription removal"() {
        given:
        def command = commandFactory.removeSubscription(new TopicName("group", "topic"),
            "subscription")
        command.backup(client)
        command.execute(client)
        wait.untilZookeeperPathNotExists(path)

        when:
        command.rollback(client)

        then:
        wait.untilZookeeperPathIsCreated(path)
        assertions.zookeeperPathContains(path, subscription)
    }
}
