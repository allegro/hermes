package pl.allegro.tech.hermes.consumers.consumer.sender

import spock.lang.Specification

import java.util.concurrent.TimeoutException

import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.failedResult
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.ofStatusCode
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.retryAfter
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.succeededResult
import static pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult.tooManyRequests

class MultiMessageSendingResultTest extends Specification {

    def "should get minimal retry time from children"() {
        given:
        def children = timeouts.collect { t -> retryAfter(t) }

        expect:
        new MultiMessageSendingResult(children).getRetryAfterMillis().get() == 1000L * expectedRetryAfter

        where:
        timeouts  | expectedRetryAfter
        [1, 2, 3] | 1
        [2]       | 2
        [1, 1, 1] | 1
    }

    def "should get minimal retry time from children for too many requests"() {
        given:
        def children = timeouts.collect { t -> tooManyRequests(t) }

        expect:
        new MultiMessageSendingResult(children).getRetryAfterMillis().get() == 1000L * expectedRetryAfter

        where:
        timeouts  | expectedRetryAfter
        [1, 2, 3] | 1
        [2]       | 2
        [1, 1, 1] | 1
    }

    def "should return client error if all failed children have client error"() {
        given:
        def children = [ofStatusCode(400), ofStatusCode(403), ofStatusCode(200)]

        expect:
        new MultiMessageSendingResult(children).isClientError()
    }

    def "should return timeout if any of children has timeout"() {
        given:
        def children = [failedResult(new TimeoutException()), ofStatusCode(200)]

        expect:
        new MultiMessageSendingResult(children).isTimeout()
    }

    def "should return retry later if any of children has retry later"() {
        given:
        def children = [retryAfter(1), ofStatusCode(200)]

        expect:
        new MultiMessageSendingResult(children).isRetryLater()
    }

    def "should return retry later if any of children has too many requests"() {
        given:
        def children = [tooManyRequests(1), ofStatusCode(200)]

        expect:
        new MultiMessageSendingResult(children).isRetryLater()
    }

    def "should be loggable if one of children is loggable"() {
        given:
        def children = [failedResult(new TimeoutException()), failedResult(new Exception())]

        expect:
        new MultiMessageSendingResult(children).isLoggable()
    }

    def "should not be loggable if none of children is loggable"() {
        given:
        def children = [failedResult(new TimeoutException()), failedResult(new TimeoutException())]

        expect:
        !new MultiMessageSendingResult(children).isLoggable()
    }

    def "should return best suited status code from children"() {
        given:
        def children = statusCodes.collect { statusCode -> ofStatusCode(statusCode) }

        expect:
        new MultiMessageSendingResult(children).getStatusCode() == expectedStatusCode

        where:
        statusCodes     | expectedStatusCode
        [200, 201, 200] | 201
        [0, 200, 200]   | 0
        [200, 201, 400] | 400
        [200, 400, 503] | 503
    }

    def "should return log info for all children"() {
        def child1 = Stub(SingleMessageSendingResult) {
            getRequestUri() >> Optional.of(URI.create("http://url1"))
            getRootCause() >> "error1"
            getFailure() >> new Exception("exception1")
        }

        def child2 = Stub(SingleMessageSendingResult) {
            getRequestUri() >> Optional.of(URI.create("http://url2"))
            getRootCause() >> "error2"
            getFailure() >> new Exception("exception2")
        }

        def messageSendingResult = new MultiMessageSendingResult([child1, child2])

        expect:
        messageSendingResult.getLogInfo().rootCause == ["error1", "error2"]
        messageSendingResult.getLogInfo().url.value.collect { it.toString() } == ["http://url1", "http://url2"]
        messageSendingResult.getLogInfo().failure.message == ["exception1", "exception2"]
    }

    def "should build exception root cause from all children"() {
        given:
        def child1 = Stub(SingleMessageSendingResult) {
            getRequestUri() >> Optional.of(URI.create("http://url1"))
            getRootCause() >> "error1"
        }

        def child2 = Stub(SingleMessageSendingResult) {
            getRequestUri() >> Optional.of(URI.create("http://url2"))
            getRootCause() >> "error2"
        }

        def messageSendingResult = new MultiMessageSendingResult([child1, child2])

        expect:
        messageSendingResult.getRootCause() == "http://url1:error1;http://url2:error2"
    }

    def "should retun only succeeded url filtered by predicate"() {
        given:
        def messageSendingResult = new MultiMessageSendingResult([succeededResult(URI.create("http://url1")), failedResult(400), failedResult(500)])
        def predicate = { MessageSendingResult result -> result.getStatusCode() == 200 }

        expect:
        messageSendingResult.getSucceededUris(predicate).size() == 1
    }


}
