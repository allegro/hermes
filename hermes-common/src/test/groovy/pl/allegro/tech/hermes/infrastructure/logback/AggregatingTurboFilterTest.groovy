package pl.allegro.tech.hermes.infrastructure.logback

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.Appender
import ch.qos.logback.core.spi.FilterReply
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AggregatingTurboFilterTest extends Specification {

    def "should pass through messages from loggers without aggregation enabled"() {
        given:
        def filter = createFilter("some-aggregated-logger")
        def notAggregatedLogger = getLogger("not-aggregated")

        when:
        def reply = filter.decide(null, notAggregatedLogger, Level.ERROR, "Some problem", null, null)

        then:
        reply == FilterReply.NEUTRAL
    }

    def "should report aggregated messages for configured logger"() {
        given:
        def filter = createFilter("some-aggregated-logger")
        filter.reportingIntervalMillis = 50
        filter.start()

        def aggregatedLogger = getLogger("some-aggregated-logger")
        def appenderCalled = new BlockingVariable<Boolean>(200, TimeUnit.MILLISECONDS)
        LoggingEvent capturedEvent = null

        def appender = [doAppend: { event ->
            capturedEvent = event
            appenderCalled.set(true)
        }] as Appender
        aggregatedLogger.addAppender(appender)

        when:
        def reply1 = filter.decide(null, aggregatedLogger, Level.ERROR, "Some problem", null, null)
        def reply2 = filter.decide(null, aggregatedLogger, Level.ERROR, "Some problem", null, null)

        then: "the messages are denied by the filter"
        reply1 == FilterReply.DENY
        reply2 == FilterReply.DENY

        and: "later an aggregate is logged asynchronously by another thread"
        appenderCalled.get()
        capturedEvent.level == Level.ERROR
        capturedEvent.message == "Some problem [occurrences=2]"

        cleanup:
        filter.stop()
    }

    def "should report aggregated messages grouping by params"() {
        given:
        def filter = createFilter("some-aggregated-logger")
        filter.reportingIntervalMillis = 50
        filter.start()

        def appender = Mock(Appender)
        def aggregatedLogger = getLogger("some-aggregated-logger")
        aggregatedLogger.addAppender(appender)
        List<LoggingEvent> capturedEvents = []

        when:
        5.times {
            filter.decide(null, aggregatedLogger, Level.ERROR, "Hello {}", (Object[])["James"], null)
        }
        2.times {
            filter.decide(null, aggregatedLogger, Level.ERROR, "Hello {}", (Object[])["Mary"], null)
        }

        and:
        filter.report()

        then:
        2 * appender.doAppend(_) >> { LoggingEvent event ->
            capturedEvents << event
        }
        capturedEvents.count { it.message == "Hello {} [occurrences=5]" && it.argumentArray[0] == "James"} == 1
        capturedEvents.count { it.message == "Hello {} [occurrences=2]" && it.argumentArray[0] == "Mary"} == 1
    }

    def "should log aggregates with the same log level as original messages"() {
        given:
        def filter = createFilter("some-aggregated-logger")

        def appender = Mock(Appender)
        def aggregatedLogger = getLogger("some-aggregated-logger")
        aggregatedLogger.addAppender(appender)
        List<LoggingEvent> capturedEvents = []

        when:
        filter.decide(null, aggregatedLogger, Level.WARN, "Some problem", null, null)
        filter.decide(null, aggregatedLogger, Level.ERROR, "Some problem", null, null)

        and:
        filter.report()

        then:
        2 * appender.doAppend(_) >> { LoggingEvent event ->
            capturedEvents << event
        }
        capturedEvents.count { it.message == "Some problem [occurrences=1]" } == 2
        capturedEvents.count { it.level == Level.WARN } == 1
        capturedEvents.count { it.level == Level.ERROR } == 1
    }

    def "should log aggregates with the same marker as for original messages"() {
        given:
        def filter = createFilter("some-aggregated-logger")

        def appender = Mock(Appender)
        def aggregatedLogger = getLogger("some-aggregated-logger")
        aggregatedLogger.addAppender(appender)
        List<LoggingEvent> capturedEvents = []
        def myMarker = MarkerFactory.getMarker("abc")

        when:
        filter.decide(myMarker, aggregatedLogger, Level.WARN, "Some problem", null, null)
        filter.decide(null, aggregatedLogger, Level.WARN, "Some problem", null, null)

        and:
        filter.report()

        then:
        2 * appender.doAppend(_) >> { LoggingEvent event ->
            capturedEvents << event
        }
        capturedEvents.count { it.message == "Some problem [occurrences=1]" } == 2
        capturedEvents.count { it.marker == myMarker } == 1
        capturedEvents.count { it.marker == AggregatingTurboFilter.MARKER } == 1
    }

    def "should log last exception"() {
        given:
        def filter = createFilter("some-aggregated-logger")

        def aggregatedLogger = getLogger("some-aggregated-logger")
        def appender = Mock(Appender)
        aggregatedLogger.addAppender(appender)

        when:
        filter.decide(null, aggregatedLogger, Level.ERROR, "Some problem", null, new RuntimeException("forgotten exception"))
        filter.decide(null, aggregatedLogger, Level.ERROR, "Some problem", null, new RuntimeException("saved exception"))
        filter.decide(null, aggregatedLogger, Level.ERROR, "Some problem", null, null)

        and:
        filter.report()

        then:
        1 * appender.doAppend(_) >> { LoggingEvent event ->
            assert event.level == Level.ERROR
            assert event.message == "Some problem [occurrences=3]"
            assert event.throwableProxy.message == "saved exception"
        }
    }

    def "should not report when there are no more logs"() {
        given:
        def filter = createFilter("some-aggregated-logger")

        def aggregatedLogger = getLogger("some-aggregated-logger")
        def appender = Mock(Appender)
        aggregatedLogger.addAppender(appender)

        when:
        filter.decide(null, aggregatedLogger, Level.ERROR, "Some problem", null, null)

        and:
        filter.report()

        then:
        1 * appender.doAppend(_) >> { LoggingEvent event ->
            assert event.level == Level.ERROR
            assert event.message == "Some problem [occurrences=1]"
        }

        when:
        filter.report()

        then:
        0 * appender.doAppend(_)
    }

    def "should allow multithreaded access"() {
        given:
        def threadsCount = 5
        def latch = new CountDownLatch(threadsCount)
        def logsPerThread = 1_000
        def executor = Executors.newFixedThreadPool(threadsCount)

        def filter = createFilter("some-aggregated-logger")
        def aggregatedLogger = getLogger("some-aggregated-logger")
        def appender = Mock(Appender)
        aggregatedLogger.addAppender(appender)

        when:
        threadsCount.times {
            executor.submit({
                logsPerThread.times {
                    filter.decide(null, aggregatedLogger, Level.ERROR, "An error", null, null)
                }
                latch.countDown()
            })
        }

        then:
        latch.await(2, TimeUnit.SECONDS)

        and:
        filter.report()

        then:
        1 * appender.doAppend(_) >> { LoggingEvent event ->
            assert event.message == "An error [occurrences=${threadsCount * logsPerThread}]"
        }
    }

    def "should allow multithreaded access while reporting simultaneously"() {
        given:
        def loggerCalls = new AtomicInteger()
        def count = new AtomicInteger()
        def countRegexp = /An error \[occurrences\=(\d+)\]/

        def threadsCount = 5
        def latch = new CountDownLatch(threadsCount)
        def logsPerThread = 10_000
        def executor = Executors.newFixedThreadPool(threadsCount)

        def filter = createFilter("some-aggregated-logger")
        filter.setReportingIntervalMillis(10) // small reporting interval
        filter.start()

        def aggregatedLogger = getLogger("some-aggregated-logger")
        def appender = [doAppend: { LoggingEvent event ->
            loggerCalls.incrementAndGet()
            def matcher = (event.message =~ countRegexp) // we need to parse the number of occurrences
            assert matcher.matches()
            count.addAndGet(matcher.group(1) as Integer)
        }] as Appender
        aggregatedLogger.addAppender(appender)

        when:
        threadsCount.times {
            executor.submit({
                logsPerThread.times {
                    filter.decide(null, aggregatedLogger, Level.ERROR, "An error", null, null)
                }
                latch.countDown()
            })
        }

        then: "we wait for all threads to stop logging"
        latch.await(5, TimeUnit.SECONDS)

        and: "make sure all messages are flushed"
        filter.report()

        then:
        loggerCalls.get() > 1
        count.get() == threadsCount * logsPerThread

        cleanup:
        filter.stop()
    }

    AggregatingTurboFilter createFilter(String loggerName) {
        def filter = new AggregatingTurboFilter()
        filter.addAggregatedLogger(loggerName)
        return filter
    }

    Logger getLogger(String name) {
        return (Logger) LoggerFactory.getLogger(name)
    }
}
