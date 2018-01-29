package pl.allegro.tech.hermes.infrastructure.zookeeper.executor

import java.util.concurrent.Executors
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClient
import pl.allegro.tech.hermes.infrastructure.zookeeper.client.ZookeeperClientManager
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
        1 * command.backup(_ as ZookeeperClient)
        1 * command.execute(_ as ZookeeperClient)
    }

    def "should not backup executing command when rollback is disabled"() {
        given:
        def executor = buildExecutor(1, false)

        and:
        def command = Mock(ZookeeperCommand)

        when:
        executor.execute(command)

        then:
        0 * command.backup(_ as ZookeeperClient)
        1 * command.execute(_ as ZookeeperClient)
    }

    def "should throw ZookeeperOperationFailedException when backup failed"() {
        given:
        def executor = buildExecutor(1, true)

        and:
        def command = Stub(ZookeeperCommand)
        command.backup(_ as ZookeeperClient) >> { throw someException() }

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
        1 * command.backup(_ as ZookeeperClient)
    }

    def "should execute command on each client"() {
        given:
        def executor = buildExecutor(2, true)

        and:
        def command = Mock(ZookeeperCommand)

        when:
        executor.execute(command)

        then:
        2 * command.execute(_ as ZookeeperClient)
    }

    def "should throw ZookeeperCommandFailedException when command failed on any client"() {
        given:
        def succeedingClient = Stub(ZookeeperClient)
        def failingClient = Stub(ZookeeperClient)
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
        def succeedingClient = Stub(ZookeeperClient)
        def failingClient = Stub(ZookeeperClient)
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
        def succeedingClient = Stub(ZookeeperClient)
        def failingClient = Stub(ZookeeperClient)
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
        def succeedingClient = Stub(ZookeeperClient)
        def failingClient = Stub(ZookeeperClient)
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

    def someException() {
        return new RuntimeException("Execution failed.")
    }

    def buildExecutor(int clientCount, boolean rollbackEnabled) {
        def clients = stubClients(clientCount)
        return buildExecutor(clients, rollbackEnabled)
    }

    def buildExecutor(List<ZookeeperClient> clients, boolean rollbackEnabled) {
        def clientManager = Stub(ZookeeperClientManager)
        clientManager.getLocalClient() >> clients.first()
        clientManager.getClients() >> clients
        def executor = Executors.newSingleThreadExecutor()
        return new ZookeeperCommandExecutor(clientManager, executor, rollbackEnabled)
    }

    private def stubClients(int clientCount) {
        List<ZookeeperClient> clients = new ArrayList<>()
        clientCount.times { clients.add(Stub(ZookeeperClient)) }
        return clients
    }
}
