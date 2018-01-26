package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter

class CreateGroupZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client.getCuratorFramework())
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def "should create group"() {
        given:
        def group = new Group("group-name-create", "support-team")
        def command = commandFactory.createGroup(group)

        when:
        command.execute(client)

        then:
        wait.untilZookeeperPathIsCreated("/hermes/groups/group-name-create/topics")
        assertions.zookeeperPathContains("/hermes/groups/group-name-create", group)
    }

    def "should rollback group creation"() {
        given:
        def group = new Group("group-name-rollback", "support-team")

        and:
        def command = commandFactory.createGroup(group)
        command.backup(client)
        command.execute(client)
        wait.untilZookeeperPathIsCreated("/hermes/groups/group-name-rollback")

        when:
        command.rollback(client)

        then:
        wait.untilZookeeperPathNotExists("/hermes/groups/group-name-rollback")
    }


}
