package pl.allegro.tech.hermes.infrastructure.zookeeper

import org.apache.curator.framework.CuratorFramework
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommand
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.RollbackFailedException
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandExecutor
import pl.allegro.tech.hermes.infrastructure.zookeeper.executor.ZookeeperCommandFailedException
import spock.lang.Specification


class ZookeeperCommandExecutorTest extends Specification {

    def "should backup before executing command when rollback is enabled"() {
        given:
        def executor = buildExecutor(1, true)

        and:
        def command = Mock(ZookeeperCommand)

        when:
        executor.execute(command)

        then:
        1 * command.backup(_ as CuratorFramework)
        1 * command.execute(_ as CuratorFramework)
    }

    def "should not backup executing command when rollback is disabled"() {
        given:
        def executor = buildExecutor(1, false)

        and:
        def command = Mock(ZookeeperCommand)

        when:
        executor.execute(command)

        then:
        0 * command.backup(_ as CuratorFramework)
        1 * command.execute(_ as CuratorFramework)
    }

    def "should throw ZookeeperOperationFailedException when backup failed"() {
        given:
        def executor = buildExecutor(1, true)

        and:
        def command = Stub(ZookeeperCommand)
        command.backup(_ as CuratorFramework) >> { throw someException() }

        when:
        executor.execute(command)

        then:
        thrown ZookeeperCommandFailedException
    }

    def "should backup only once even if executing on multiple clients"() {
        given:
        def executor = buildExecutor(2, true)

        and:
        def command = Mock(ZookeeperCommand)

        when:
        executor.execute(command)

        then:
        1 * command.backup(_ as CuratorFramework)
    }

    def "should execute command on each client"() {
        given:
        def executor = buildExecutor(2, true)

        and:
        def command = Mock(ZookeeperCommand)

        when:
        executor.execute(command)

        then:
        2 * command.execute(_ as CuratorFramework)
    }

    def "should throw ZookeeperCommandFailedException when command failed on any client"() {
        given:
        def succeedingClient = Stub(CuratorFramework)
        def failingClient = Stub(CuratorFramework)
        def executor = buildExecutor([succeedingClient, failingClient], true)

        and:
        def command = Mock(ZookeeperCommand)
        command.execute(failingClient) >> { throw someException() }

        when:
        executor.execute(command)

        then:
        thrown ZookeeperCommandFailedException
    }

    def "should rollback only on clients which executed command successfully"() {
        given:
        def succeedingClient = Stub(CuratorFramework)
        def failingClient = Stub(CuratorFramework)
        def executor = buildExecutor([succeedingClient, failingClient], true)

        and:
        def command = Mock(ZookeeperCommand)
        command.execute(failingClient) >> { throw someException() }

        when:
        executor.execute(command)

        then:
        thrown ZookeeperCommandFailedException
        1 * command.rollback(succeedingClient)
        0 * command.rollback(failingClient)
    }

    def "should not rollback when rollback is disabled"() {
        given:
        def succeedingClient = Stub(CuratorFramework)
        def failingClient = Stub(CuratorFramework)
        def executor = buildExecutor([succeedingClient, failingClient], false)

        and:
        def command = Mock(ZookeeperCommand)

        when:
        executor.execute(command)

        then:
        0 * command.rollback(succeedingClient)
    }

    def "should throw RollbackFailedException when rollback procedure failed"() {
        given:
        def succeedingClient = Stub(CuratorFramework)
        def failingClient = Stub(CuratorFramework)
        def executor = buildExecutor([succeedingClient, failingClient], true)

        and:
        def command = Stub(ZookeeperCommand)
        command.execute(failingClient) >> { throw someException() }
        command.rollback(succeedingClient) >> { throw someException() }

        when:
        executor.execute(command)

        then:
        thrown RollbackFailedException
    }

    def buildExecutor(List<CuratorFramework> clients, boolean rollbackEnabled) {
        def clientManager = new ZookeeperClientManager(clients)
        return new ZookeeperCommandExecutor(clientManager, 1, rollbackEnabled)
    }

    def buildExecutor(int clientCount, boolean rollbackEnabled) {
        def clients = new ArrayList<CuratorFramework>()
        clientCount.times { clients.add(Stub(CuratorFramework)) }
        return buildExecutor(clients, rollbackEnabled)
    }

    def someException() {
        return new RuntimeException("Execution failed.")
    }
}
