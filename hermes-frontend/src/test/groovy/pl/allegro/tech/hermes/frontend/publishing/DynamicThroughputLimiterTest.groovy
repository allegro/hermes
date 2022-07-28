package pl.allegro.tech.hermes.frontend.publishing

import com.codahale.metrics.Metered
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.frontend.publishing.handlers.DynamicThroughputLimiter
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.ScheduledExecutorService

class DynamicThroughputLimiterTest extends Specification {

    def max = 10_000
    def threshold = 8000
    def desired = 6000
    def idleThreshold = 0.5

    double globalRate = 0.0
    def globalMeter = [getOneMinuteRate: {globalRate}] as Metered

    def topicName = new TopicName("group", "name")
    def executor = Mock(ScheduledExecutorService)

    def limiter = new DynamicThroughputLimiter(max, threshold, desired, idleThreshold, Duration.ofSeconds(1), globalMeter, executor)


    def "should grant quota if global rate is below max"() {
        given:
        globalRate = 10

        expect:
        limiter.checkQuota(topicName, meter(1000)).hasQuota()
    }

    def "should not grant quota if global rate is above max"() {
        given:
        globalRate = 12_000

        when:
        def quota = limiter.checkQuota(topicName, meter(1000))

        then:
        !quota.hasQuota()
        quota.getReason().contains("Global")
    }

    def "should block single abuser among multiple users"() {
        given:
        def meter = meter(1000)
        def topic1 = registerTopic("group1", "name1", meter)
        def topic2 = registerTopic("group2", "name2", meter)

        double abuserRate = 7000
        def abuserMeter = [getOneMinuteRate: {abuserRate}, getFifteenMinuteRate: {abuserRate}] as Metered
        def abuser = registerTopic("group3", "name3", abuserMeter)

        limiter.run()

        abuserRate = 2001

        expect:
        !limiter.checkQuota(abuser, abuserMeter).hasQuota()
        limiter.checkQuota(topic1, meter).hasQuota()
        limiter.checkQuota(topic2, meter).hasQuota()
    }

    def "should block multiple abusers among multiple users"() {
        given:
        def meter1 = meter(4500)
        def abuser1 = registerTopic("group1", "name1", meter1)
        def meter2 = meter(4500)
        def abuser2 = registerTopic("group2", "name2", meter2)
        def meter3 = meter(500)
        def innocent = registerTopic("group3", "name3", meter3)

        limiter.run()

        expect:
        !limiter.checkQuota(abuser1, meter1).hasQuota()
        !limiter.checkQuota(abuser2, meter2).hasQuota()
        limiter.checkQuota(innocent, meter3).hasQuota()
    }

    def "should forget about violations if user was idle"() {
        given:
        double abuserRate = 9000
        def abuserMeter = [getOneMinuteRate: {abuserRate}] as Metered
        def abuser = registerTopic("group1", "name1", abuserMeter)
        limiter.run()
        assert !limiter.checkQuota(abuser, abuserMeter).hasQuota()

        when:
        abuserRate = 0.1
        limiter.run()
        abuserRate = 9000

        then:
        limiter.checkQuota(abuser, abuserMeter).hasQuota()
    }

    private Metered meter(double rate) {
        Mock(Metered) { getOneMinuteRate() >> rate; getFifteenMinuteRate() >> rate }
    }

    private TopicName registerTopic(String group, String name, Metered rate) {
        def topic = new TopicName(group, name)
        globalRate += rate.getOneMinuteRate()
        limiter.checkQuota(topic, rate)
        topic
    }
}
