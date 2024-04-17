package pl.allegro.tech.hermes.frontend.utils

import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

import static java.util.concurrent.CompletableFuture.completedFuture

class CompletableFuturesHelperTest extends Specification {

    def "should return completable future completing when all underlying futures complete"() {
        given:
        def future1 = completedFuture("abc")
        def future2 = completedFuture("def")
        def future3 = completedFuture("ghi")

        def allFutures = [future1, future2, future3]

        when:
        def allCompleteFuture = CompletableFuturesHelper.allComplete(allFutures)

        then:
        allCompleteFuture.join() == ["abc", "def", "ghi"]
    }

    def "should not block current thread when underlying futures don't complete"() {
        given:
        def future1 = new CompletableFuture()
        def future2 = completedFuture("abc")

        def allFutures = [future1, future2]

        when:
        def allCompleteFuture = CompletableFuturesHelper.allComplete(allFutures)

        then:
        !allCompleteFuture.isDone()
    }

    def "should complete exceptionally if any of futures complete so"() {
        given:
        def future1FailureCause = new RuntimeException("an error")
        def future1 = new CompletableFuture()
        future1.completeExceptionally(future1FailureCause)
        def future2 = completedFuture("abc")

        def allFutures = [future1, future2]

        when:
        def allCompleteFuture = CompletableFuturesHelper.allComplete(allFutures)

        then:
        allCompleteFuture.completedExceptionally

        when:
        allCompleteFuture.join()

        then:
        def e = thrown CompletionException
        e.cause == future1FailureCause
    }

}
