package pl.allegro.tech.hermes.management.domain.dc

import pl.allegro.tech.hermes.common.exception.InternalProcessingException
import pl.allegro.tech.hermes.domain.group.GroupRepository
import spock.lang.Specification


class MultiDcRepositoryCommandExecutorTest extends Specification {

    def "should execute backup if rollback is enabled"() {
        given:
        def executor = buildExecutor(true)
        def command = Mock(RepositoryCommand)

        when:
        executor.execute(command)

        then:
        1 * command.backup(_)
    }

    def "should not execute backup if rollback is disabled"() {
        given:
        def executor = buildExecutor(false)
        def command = Mock(RepositoryCommand)

        when:
        executor.execute(command)

        then:
        0 * command.backup(_)
    }

    def "should execute command on all repositories"() {
        given:
        def repository1 = Stub(GroupRepository)
        def repository2 = Stub(GroupRepository)

        def executor = buildExecutor([repository1, repository2], true)

        def command = Mock(RepositoryCommand)

        when:
        executor.execute(command)

        then:
        1 * command.execute(repository1)
        1 * command.execute(repository2)
    }

    def "should rollback if execution failed on second repository"() {
        given:
        def repository1 = Stub(GroupRepository)
        def repository2 = Stub(GroupRepository)

        def executor = buildExecutor([repository1, repository2], true)

        def command = Mock(RepositoryCommand)
        command.execute(repository2) >> { throw new Exception() }

        when:
        executor.execute(command)

        then:
        1 * command.rollback(repository1)

        thrown InternalProcessingException
    }

    def "should not rollback if rollback is disabled"() {
        given:
        def repository1 = Stub(GroupRepository)
        def repository2 = Stub(GroupRepository)

        def executor = buildExecutor([repository1, repository2], false)

        def command = Mock(RepositoryCommand)
        command.execute(repository2) >> { throw new Exception() }

        when:
        executor.execute(command)

        then:
        0 * command.rollback(repository1)

        thrown InternalProcessingException
    }

    private buildExecutor(boolean rollbackEnabled) {
        def repositoryManager = Stub(RepositoryManager)
        return new MultiDcRepositoryCommandExecutor(repositoryManager, rollbackEnabled)
    }

    private buildExecutor(List repositories, boolean rollbackEnabled) {
        def repositoryManager = Stub(RepositoryManager)
        repositoryManager.getRepositories(_) >>
            repositories.collect({ new DcBoundRepositoryHolder(it, "dc-name") })
        return new MultiDcRepositoryCommandExecutor(repositoryManager, rollbackEnabled)
    }

}
