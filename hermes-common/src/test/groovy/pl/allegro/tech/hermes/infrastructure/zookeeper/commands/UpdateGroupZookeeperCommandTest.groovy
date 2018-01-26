package pl.allegro.tech.hermes.infrastructure.zookeeper.commands

import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.test.IntegrationTest
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperWaiter


class UpdateGroupZookeeperCommandTest extends IntegrationTest {

    def client = zookeeper()
    def wait = new ZookeeperWaiter(client)

    def "should update group"() {
        given:
        def oldGroup = new Group("group-name-update", "old-support-team")
        def newGroup = new Group("group-name-update", "new-support-team")
        commandFactory.createGroup(oldGroup).execute(client)
        def command = commandFactory.updateGroup(newGroup)

        when:
        command.execute(client)

        then:
        getData(client, "/hermes/groups/group-name-update", Group.class) == newGroup
    }

    def "should rollback group update"() {
        given:
        def oldGroup = new Group("group-name-rollback", "old-support-team")
        def newGroup = new Group("group-name-rollback", "new-support-team")

        def createGroupCommand = commandFactory.createGroup(oldGroup)
        createGroupCommand.execute(client)

        def updateGroupCommand = commandFactory.updateGroup(newGroup)
        updateGroupCommand.backup(client)
        updateGroupCommand.execute(client)

        when:
        updateGroupCommand.rollback(client)

        then:
        getData(client, "/hermes/groups/group-name-rollback", Group.class) == oldGroup
    }
}
