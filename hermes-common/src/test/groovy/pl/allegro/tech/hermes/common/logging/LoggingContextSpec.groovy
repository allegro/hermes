package pl.allegro.tech.hermes.common.logging

import org.slf4j.MDC
import spock.lang.Specification

class LoggingContextSpec extends Specification {

    def cleanup() {
        MDC.clear()
    }

    def 'should set and remove MDC context for supplier'() {
        given:
        String key = 'test-key'
        String value = 'test-value'

        when:
        String result = LoggingContext.withLogging(key, value, { ->
            return MDC.get(key)
        })

        then:
        result == value

        and: 'MDC should be cleaned up after execution'
        MDC.get(key) == null
    }

    def 'should set and remove MDC context for runnable'() {
        given:
        String key = 'test-key'
        String value = 'test-value'
        String capturedValue = null

        when:
        LoggingContext.runWithLogging(key, value, {
            capturedValue = MDC.get(key)
        })

        then:
        capturedValue == value

        and: 'MDC should be cleaned up after execution'
        MDC.get(key) == null
    }

    def 'should clean up MDC even when supplier throws exception'() {
        given:
        String key = 'test-key'
        String value = 'test-value'

        when:
        LoggingContext.withLogging(key, value, { ->
            throw new RuntimeException('test exception')
        })

        then:
        thrown(RuntimeException)

        and: 'MDC should still be cleaned up'
        MDC.get(key) == null
    }

    def 'should clean up MDC even when runnable throws exception'() {
        given:
        String key = 'test-key'
        String value = 'test-value'

        when:
        LoggingContext.runWithLogging(key, value, {
            throw new RuntimeException('test exception')
        })

        then:
        thrown(RuntimeException)

        and: 'MDC should still be cleaned up'
        MDC.get(key) == null
    }

    def 'should return correct value from supplier'() {
        given:
        String key = 'test-key'
        String value = 'test-value'
        String expectedResult = 'result-123'

        when:
        String result = LoggingContext.withLogging(key, value, { ->
            return expectedResult
        })

        then:
        result == expectedResult
    }

    def 'should work with subscription-name field'() {
        given:
        String subscriptionName = 'group.topic\$subscription'

        when:
        String result = LoggingContext.withLogging(LoggingFields.SUBSCRIPTION_NAME, subscriptionName, { ->
            return MDC.get(LoggingFields.SUBSCRIPTION_NAME)
        })

        then:
        result == subscriptionName

        and: 'MDC should be cleaned up'
        MDC.get(LoggingFields.SUBSCRIPTION_NAME) == null
    }

    def 'should work with topic-name field'() {
        given:
        String topicName = 'group.topic'

        when:
        String result = LoggingContext.withLogging(LoggingFields.TOPIC_NAME, topicName, { ->
            return MDC.get(LoggingFields.TOPIC_NAME)
        })

        then:
        result == topicName

        and: 'MDC should be cleaned up'
        MDC.get(LoggingFields.TOPIC_NAME) == null
    }

    def 'should not affect other MDC values'() {
        given:
        String existingKey = 'existing-key'
        String existingValue = 'existing-value'
        String newKey = 'new-key'
        String newValue = 'new-value'

        MDC.put(existingKey, existingValue)

        when:
        LoggingContext.runWithLogging(newKey, newValue, {
            assert MDC.get(existingKey) == existingValue
            assert MDC.get(newKey) == newValue
        })

        then:
        MDC.get(existingKey) == existingValue
        MDC.get(newKey) == null
    }

    def 'should handle nested contexts'() {
        given:
        String outerKey = 'outer-key'
        String outerValue = 'outer-value'
        String innerKey = 'inner-key'
        String innerValue = 'inner-value'
        String capturedOuter = null
        String capturedInner = null

        when:
        LoggingContext.runWithLogging(outerKey, outerValue, {
            capturedOuter = MDC.get(outerKey)

            LoggingContext.runWithLogging(innerKey, innerValue, {
                // Both contexts should be active
                assert MDC.get(outerKey) == outerValue
                capturedInner = MDC.get(innerKey)
            })

            // After inner context, outer should still be active
            assert MDC.get(outerKey) == outerValue
            // But inner should be cleaned up
            assert MDC.get(innerKey) == null
        })

        then:
        capturedOuter == outerValue
        capturedInner == innerValue

        and: 'All contexts should be cleaned up'
        MDC.get(outerKey) == null
        MDC.get(innerKey) == null
    }

    def 'should handle null value'() {
        given:
        String key = 'test-key'

        when:
        String result = LoggingContext.withLogging(key, null, { ->
            return MDC.get(key)
        })

        then:
        result == null

        and: 'MDC should be cleaned up'
        MDC.get(key) == null
    }

    def 'should preserve exception type and message from supplier'() {
        given:
        String key = 'test-key'
        String value = 'test-value'
        String exceptionMessage = 'specific error message'

        when:
        LoggingContext.withLogging(key, value, { ->
            throw new IllegalArgumentException(exceptionMessage)
        })

        then:
        IllegalArgumentException exception = thrown(IllegalArgumentException)
        exception.message == exceptionMessage

        and: 'MDC should still be cleaned up'
        MDC.get(key) == null
    }

    def 'should preserve exception type and message from runnable'() {
        given:
        String key = 'test-key'
        String value = 'test-value'
        String exceptionMessage = 'specific error message'

        when:
        LoggingContext.runWithLogging(key, value, {
            throw new IllegalStateException(exceptionMessage)
        })

        then:
        IllegalStateException exception = thrown(IllegalStateException)
        exception.message == exceptionMessage

        and: 'MDC should still be cleaned up'
        MDC.get(key) == null
    }

    def 'should restore previous value when nesting same key with runnable'() {
        given:
        String key = 'topic-name'
        String outerValue = 'group.topic1'
        String innerValue = 'group.topic2'
        String valueInsideInner = null
        String valueAfterInner = null

        when:
        LoggingContext.runWithLogging(key, outerValue, {
            // Outer context is active
            assert MDC.get(key) == outerValue

            LoggingContext.runWithLogging(key, innerValue, {
                // Inner context overrides outer
                valueInsideInner = MDC.get(key)
            })

            // After inner completes, should restore outer value
            valueAfterInner = MDC.get(key)
        })

        then:
        valueInsideInner == innerValue
        valueAfterInner == outerValue

        and: 'MDC should be cleaned up after outer completes'
        MDC.get(key) == null
    }

    def 'should restore previous value when nesting same key with supplier'() {
        given:
        String key = 'topic-name'
        String outerValue = 'group.topic1'
        String innerValue = 'group.topic2'

        when:
        String finalResult = LoggingContext.withLogging(key, outerValue, {
            String afterOuter = MDC.get(key)

            String innerResult = LoggingContext.withLogging(key, innerValue, {
                return MDC.get(key)
            })

            String afterInner = MDC.get(key)

            return "${afterOuter}|${innerResult}|${afterInner}"
        })

        then:
        finalResult == "${outerValue}|${innerValue}|${outerValue}"

        and: 'MDC should be cleaned up'
        MDC.get(key) == null
    }

    def 'should handle multiple levels of nesting with same key'() {
        given:
        String key = 'subscription-name'
        String level1 = 'sub1'
        String level2 = 'sub2'
        String level3 = 'sub3'
        List<String> capturedValues = []

        when:
        LoggingContext.runWithLogging(key, level1, {
            capturedValues << MDC.get(key) // Should be 'sub1'

            LoggingContext.runWithLogging(key, level2, {
                capturedValues << MDC.get(key) // Should be 'sub2'

                LoggingContext.runWithLogging(key, level3, {
                    capturedValues << MDC.get(key) // Should be 'sub3'
                })

                capturedValues << MDC.get(key) // Should restore to 'sub2'
            })

            capturedValues << MDC.get(key) // Should restore to 'sub1'
        })

        then:
        capturedValues == [level1, level2, level3, level2, level1]

        and: 'MDC should be cleaned up'
        MDC.get(key) == null
    }

    def 'should restore previous value even when nested call throws exception with runnable'() {
        given:
        String key = 'topic-name'
        String outerValue = 'outer'
        String innerValue = 'inner'
        String valueAfterException = null

        when:
        LoggingContext.runWithLogging(key, outerValue, {
            try {
                LoggingContext.runWithLogging(key, innerValue, {
                    throw new RuntimeException('test exception')
                })
            } catch (RuntimeException e) {
                // Exception caught, check if value was restored
                valueAfterException = MDC.get(key)
            }
        })

        then:
        valueAfterException == outerValue // Should be restored despite exception

        and: 'MDC should be cleaned up'
        MDC.get(key) == null
    }

    def 'should restore previous value even when nested call throws exception with supplier'() {
        given:
        String key = 'topic-name'
        String outerValue = 'outer'
        String innerValue = 'inner'

        when:
        String result = LoggingContext.withLogging(key, outerValue, {
            try {
                LoggingContext.withLogging(key, innerValue, {
                    throw new RuntimeException('test exception')
                })
            } catch (RuntimeException e) {
                // Exception caught, return current MDC value
                return MDC.get(key)
            }
        })

        then:
        result == outerValue // Should be restored despite exception

        and: 'MDC should be cleaned up'
        MDC.get(key) == null
    }
}
