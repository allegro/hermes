package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperAssertions
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter


class RemoveGroupZookeeperCommandTest extends IntegrationTest {

    def client = zookeeperClient()
    def wait = new ZookeeperWaiter(client.getCuratorFramework())
    def assertions = new ZookeeperAssertions(client.getCuratorFramework(), mapper)

    def "should remove group"() {
        given:
        def group = new Group("group-name-remove", "support-team")

        and:
        def createGroupCommand = commandFactory.createGroup(group)
        createGroupCommand.execute(client)

        and:
        def removeGroupCommand = commandFactory.removeGroup("group-name-remove")

        when:
        removeGroupCommand.execute(client)

        then:
        wait.untilZookeeperPathNotExists("/hermes/groups/group-name-remove")
    }

    def "should rollback group removal"() {
        given:
        def group = new Group("group-name-rollback", "support-team")

        and:
        def createGroupCommand = commandFactory.createGroup(group)
        createGroupCommand.execute(client)

        and:
        def removeGroupCommand = commandFactory.removeGroup("group-name-rollback")
        removeGroupCommand.backup(client)
        removeGroupCommand.execute(client)

        when:
        removeGroupCommand.rollback(client)

        then:
        wait.untilZookeeperPathIsCreated("/hermes/groups/group-name-rollback")
        assertions.zookeeperPathContains("/hermes/groups/group-name-rollback", group)

        wait.untilZookeeperPathIsCreated("/hermes/groups/group-name-rollback/topics")
    }

}
