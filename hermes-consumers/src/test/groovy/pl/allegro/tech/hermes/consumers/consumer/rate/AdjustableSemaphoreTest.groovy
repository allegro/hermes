package pl.allegro.tech.hermes.consumers.consumer.rate

import spock.lang.Specification

class AdjustableSemaphoreTest extends Specification {

    def "should decrease max permits"() {
        given:
        def semaphore = new AdjustableSemaphore(2)
        semaphore.acquire()

        when:
        semaphore.setMaxPermits(1)

        then:
        semaphore.availablePermits() == 0
    }

    def "should increase max permits"() {
        given:
        def semaphore = new AdjustableSemaphore(1)
        semaphore.acquire()

        when:
        semaphore.setMaxPermits(2)

        then:
        semaphore.availablePermits() == 1
    }

    def "should tolerate same value"() {
        given:
        def semaphore = new AdjustableSemaphore(1)
        semaphore.acquire()

        when:
        semaphore.setMaxPermits(1)

        then:
        semaphore.availablePermits() == 0
    }

    def "should expose view with release-only functionality"() {
        given:
        def semaphore = new AdjustableSemaphore(2)
        InflightsPool releasable = { semaphore.release(); }

        when:
        semaphore.acquire()
        releasable.release()

        then:
        semaphore.availablePermits() == 2
        !(releasable instanceof AdjustableSemaphore)
    }
}
