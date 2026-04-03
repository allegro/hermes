package pl.allegro.tech.hermes.frontend.producer.kafka

import spock.lang.Specification

class KafkaMessageSenderPoolRouterTest extends Specification {

    def "should return zero for pool size one"() {
        given:
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(1)

        expect:
        router.route("group.topic1") == 0
        router.route("group.topic2") == 0
        router.route("any.topic") == 0
    }

    def "should return deterministic result for same topic name"() {
        given:
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(4)
        String topicName = "pl.allegro.offer.OfferCreated"

        expect:
        router.route(topicName) == router.route(topicName)
        router.route(topicName) == router.route(topicName)
    }

    def "should return index within pool bounds"() {
        given:
        int poolSize = 5
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)

        expect:
        router.route(topicName) >= 0
        router.route(topicName) < poolSize

        where:
        topicName << [
                "group1.topic1",
                "group2.topic2",
                "group3.topic3",
                "pl.allegro.offer.OfferCreated",
                "pl.allegro.user.UserRegistered",
                "some.negative.hashcode.topic",
                "",
                "a",
                "very.long.topic.name.with.many.parts.that.is.unlikely.to.exist"
        ]
    }

    def "should distribute topics across pool"() {
        given:
        int poolSize = 4
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)
        Map<Integer, Integer> distribution = new HashMap<Integer, Integer>()

        when:
        (0..<100).each { i ->
            int index = router.route("group.topic" + i)
            distribution.merge(index, 1, Integer::sum)
        }

        then:
        distribution.keySet().size() == poolSize
        (0..<poolSize).every { { distribution.containsKey(it) && distribution.get(it) > 0 } }
    }

    def "should return non-negative index even for negative hash codes"() {
        given:
        int poolSize = 3
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)
        String topicWithNegativeHash = findTopicWithNegativeHashCode()

        expect:
        topicWithNegativeHash.hashCode() < 0
        router.route(topicWithNegativeHash) >= 0
        router.route(topicWithNegativeHash) < poolSize
    }

    def "should reject pool size less than one"() {
        when:
        new KafkaMessageSenderPoolRouter(poolSize)

        then:
        Exception e = thrown(IllegalArgumentException)
        e.message.contains("Pool size must be at least 1")

        where:
        poolSize << [0, -1]
    }

    def "should return configured pool size"() {
        given:
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(7)

        expect:
        router.poolSize() == 7
    }

    def "should distribute topics roughly evenly across pool members"() {
        given:
        int poolSize = 8
        int topicCount = 10000
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)
        int[] distribution = new int[poolSize]

        when:
        (0..<topicCount).each { i ->
            int index = router.route("group${i % 50}.topic${i}")
            distribution[index]++
        }

        then: "each pool index should get between 5% and 25% of topics (expected ~12.5% for 8 members)"
        double expectedShare = topicCount / poolSize
        double tolerance = 0.50 // allow 50% deviation from expected share
        (0..<poolSize).every {
            distribution[it] >= expectedShare * (1 - tolerance) &&
                    distribution[it] <= expectedShare * (1 + tolerance)
        }
    }

    private static String findTopicWithNegativeHashCode() {
        for (int i = 0; i < 10000; i++) {
            String candidate = "topic.negative." + i
            if (candidate.hashCode() < 0) {
                return candidate
            }
        }
        return "AaAa"
    }
}
