package pl.allegro.tech.hermes.infrastructure.zookeeper.executor

import spock.lang.Specification


class ExecutionResultsTest extends Specification {

    def "should return true if at least one execution failed"() {
        given:
        def results = new ExecutionResults([ExecutionResult.success(), ExecutionResult.failure(new Exception())])

        expect:
        results.executionFailed()
    }

    def "should return false if all executions succeeded"() {
        given:
        def results = new ExecutionResults([ExecutionResult.success(), ExecutionResult.success()])

        expect:
        !results.executionFailed()
    }

    def "should return list of exception for failed executions"() {
        given:
        def exception1 = new Exception()
        def exception2 = new Exception()
        def results = new ExecutionResults([
            ExecutionResult.failure(exception1),
            ExecutionResult.failure(exception2),
            ExecutionResult.success()
        ])

        when:
        def exceptions = results.getExceptions()

        then:
        exceptions == [exception1, exception2]
    }

    def "should return correct execution number"() {
        given:
        def results = new ExecutionResults([ExecutionResult.success(), ExecutionResult.failure(new Exception())])

        expect:
        results.getExecutionNumber() == 2
    }

}
