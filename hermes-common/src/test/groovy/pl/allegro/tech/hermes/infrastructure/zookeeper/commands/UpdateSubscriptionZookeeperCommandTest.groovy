package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class UpdateSubscriptionZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def setup() {
        createGroupIfNotExists("group")
        createTopicIfNotExists("topic", "group")
    }

    def "should update subscription"() {
        given:
        def oldSubscription = buildSubscription("subscription-update", "old-desc")
        def newSubscription = buildSubscription("subscription-update", "new-desc")

        and:
        def path = "/hermes/groups/group/topics/topic/subscriptions/subscription-update"

        and:
        createSubscription(oldSubscription)

        and:
        def command = commandFactory.updateSubscription(newSubscription)

        when:
        command.execute(client)

        then:
        assertions.zookeeperPathContains(path, newSubscription)
    }

    def "should rollback subscription update"() {
        given:
        def oldSubscription = buildSubscription("subscription-rollback", "old-desc")
        def newSubscription = buildSubscription("subscription-rollback", "new-desc")

        and:
        def path = "/hermes/groups/group/topics/topic/subscriptions/subscription-rollback"

        and:
        createSubscription(oldSubscription)

        and:
        def command = commandFactory.updateSubscription(newSubscription)
        command.backup(client)
        command.execute(client)

        when:
        command.rollback(client)

        then:
        assertions.zookeeperPathContains(path, oldSubscription)
    }

    private static buildSubscription(String name, String description) {
        return subscription(new TopicName("group", "topic"), name).withDescription(description).build()
    }

    private def createSubscription(Subscription subscription) {
        commandFactory.createSubscription(subscription).execute(client)
    }

}
