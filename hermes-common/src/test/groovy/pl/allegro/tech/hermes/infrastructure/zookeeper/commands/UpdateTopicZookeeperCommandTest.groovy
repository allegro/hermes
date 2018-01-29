package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.*


class UpdateTopicZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def setup() {
        createGroupIfNotExists("group")
    }

    def "should update topic"() {
        given:
        def oldTopic = buildTopic("topic-update", "old-desc")
        def newTopic = buildTopic("topic-update", "new-desc")

        and:
        def path = "/hermes/groups/group/topics/topic-update"

        and:
        createTopic(oldTopic)

        and:
        def command = commandFactory.updateTopic(newTopic)

        when:
        command.execute(client)

        then:
        assertions.zookeeperPathContains(path, newTopic)
    }

    def "should rollback topic update"() {
        given:
        def oldTopic = buildTopic("topic-rollback", "old-desc")
        def newTopic = buildTopic("topic-rollback", "new-desc")

        and:
        def path = "/hermes/groups/group/topics/topic-rollback"

        and:
        createTopic(oldTopic)

        and:
        def command = commandFactory.updateTopic(newTopic)
        command.backup(client)
        command.execute(client)

        when:
        command.rollback(client)

        then:
        assertions.zookeeperPathContains(path, oldTopic)
    }

    private static buildTopic(String name, String description) {
        topic("group", name).withDescription(description).build()
    }

    private def createTopic(Topic topic) {
        commandFactory.createTopic(topic).execute(client)
    }

}
