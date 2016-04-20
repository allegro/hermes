package pl.allegro.tech.hermes.consumers.consumer.filtering

import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.filtering.chain.FilterChainFactory
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate

import static pl.allegro.tech.hermes.consumers.test.MessageBuilder.testMessage
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class FilterChainSpec extends Specification {

    AtomicInteger counter = new AtomicInteger(0)

    def subscriptionFilter1 = new RecordingSubscriptionMessageFilterCompiler("s1", counter)
    def subscriptionFilter2 = new RecordingSubscriptionMessageFilterCompiler("s2", counter)
    def globalFilter1 = new RecordingSubscriptionMessageFilterCompiler("g1", counter)
    def globalFilter2 = new RecordingSubscriptionMessageFilterCompiler("g2", counter)

    def filterSource = new MessageFilters([globalFilter1, globalFilter2], [subscriptionFilter1, subscriptionFilter2])

    def brokenFilter = new BrokenSubscriptionMessageFilterCompiler("b1", counter)

    def "should apply global filters before subscription specific"() {
        given:
        def subscription = subscription("g.topic", "subscription")
                .withFilter(new MessageFilterSpecification(["type":"$subscriptionFilter1.type".toString()])).build()

        when:
        def result = new FilterChainFactory(filterSource).create(subscription).apply(testMessage())

        then:
        !result.filtered
        globalFilter1.tested && globalFilter1.order == 0
        globalFilter2.tested && globalFilter2.order == 1
        subscriptionFilter1.tested && subscriptionFilter1.order == 2
        !subscriptionFilter2.tested
    }

    def "should throw exception for non-existing filter"() {
        given:
        def subscription = subscription("g.topic", "subscription").withFilter(new MessageFilterSpecification(["type":"foo"])).build()

        when:
        new FilterChainFactory(filterSource).create(subscription).apply(testMessage())

        then:
        thrown NoSuchFilterException
    }

    def "should apply global filters even without subscription specific"() {
        given:
        def subscription = subscription("g.topic", "subscription").build()

        when:
        def result = new FilterChainFactory(filterSource).create(subscription).apply(testMessage())

        then:
        !result.filtered
        globalFilter1.tested && globalFilter1.order == 0
        globalFilter2.tested && globalFilter2.order == 1
        !subscriptionFilter1.tested
        !subscriptionFilter2.tested
    }

    def "should apply subscription specific filters if no global filters are declared"() {
        given:
        def filterSource = new MessageFilters([], [subscriptionFilter1, subscriptionFilter2])
        def subscription = subscription("g.topic", "subscription")
                .withFilter(new MessageFilterSpecification(["type":"$subscriptionFilter1.type".toString()]))
                .withFilter(new MessageFilterSpecification(["type":"$subscriptionFilter2.type".toString()]))
                .build();

        when:
        new FilterChainFactory(filterSource).create(subscription).apply(testMessage())

        then:
        subscriptionFilter1.tested && subscriptionFilter1.order == 0
        subscriptionFilter2.tested && subscriptionFilter2.order == 1
    }

    def "should pass message for no filters"() {
        given:
        def filterSource = new MessageFilters([], [])
        def subscription = subscription("g.topic", "subscription").build()

        expect:
        new FilterChainFactory(filterSource).create(subscription).apply(testMessage())
    }

    def "should not pass message if filter is throwing exception"() {
        given:
        def filterSource = new MessageFilters([brokenFilter], [])
        def subscription = subscription("g.topic", "subscription").build()

        when:
        def result = new FilterChainFactory(filterSource).create(subscription).apply(testMessage())

        then:
        result.filtered
        result.filterType.get() == brokenFilter.type
    }

    def "should break on first filter that is failing"() {
        given:
        def filterSource = new MessageFilters([globalFilter1, brokenFilter], [subscriptionFilter1, subscriptionFilter2])
        def subscription = subscription("g.topic", "subscription")
                .withFilter(new MessageFilterSpecification(["type":"$subscriptionFilter1.type".toString()])).build()

        when:
        def result = new FilterChainFactory(filterSource).create(subscription).apply(testMessage())

        then:
        result.filtered
        result.filterType.get() == brokenFilter.type
        globalFilter1.tested && globalFilter1.order == 0
        brokenFilter.tested && brokenFilter.order == 1
        !subscriptionFilter1.tested
        !subscriptionFilter2.tested
    }

    static class RecordingSubscriptionMessageFilterCompiler extends MessageFilter implements SubscriptionMessageFilterCompiler {
        boolean compiled = false
        boolean tested = false
        AtomicInteger counter
        def order

        RecordingSubscriptionMessageFilterCompiler(String type, AtomicInteger counter) {
            super(type, { m -> test(m) })
            this.counter = counter
        }

        @Override
        Predicate<Message> compile(MessageFilterSpecification specification) {
            compiled = true
            this
        }

        @Override
        boolean test(Message message) {
            order = counter.getAndIncrement()
            tested = true
            tested
        }
    }

    static class BrokenSubscriptionMessageFilterCompiler extends RecordingSubscriptionMessageFilterCompiler {
        BrokenSubscriptionMessageFilterCompiler(String type, AtomicInteger counter) {
            super(type, counter)
        }

        @Override
        boolean test(Message message) {
            super.test(message)
            throw new IllegalStateException()
        }
    }
}
