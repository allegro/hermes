package pl.allegro.tech.hermes.frontend.producer.kafka

import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

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

    def "should assign topics in round-robin order"() {
        given:
        int poolSize = 4
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)

        expect:
        router.route("topic.a") == 0
        router.route("topic.b") == 1
        router.route("topic.c") == 2
        router.route("topic.d") == 3
        router.route("topic.e") == 0
        router.route("topic.f") == 1
    }

    def "should not change assignment for already seen topic"() {
        given:
        int poolSize = 3
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)

        when:
        int firstCall = router.route("topic.a")
        router.route("topic.b")
        router.route("topic.c")
        int secondCall = router.route("topic.a")

        then:
        firstCall == 0
        secondCall == 0
    }

    def "should distribute topics across pool"() {
        given:
        int poolSize = 4
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)
        Map<Integer, Integer> distribution = new HashMap<Integer, Integer>()

        when:
        (0..<1000).each { i ->
            int index = router.route("group.topic" + i)
            distribution.merge(index, 1, Integer::sum)
        }

        then:
        distribution.keySet().size() == poolSize
        (0..<poolSize).every { distribution.containsKey(it) && distribution.get(it) == 250 }
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

    def "should distribute topics perfectly evenly when topic count is divisible by pool size"() {
        given:
        int poolSize = 8
        int topicCount = 800
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)
        int[] distribution = new int[poolSize]

        when:
        (0..<topicCount).each { i ->
            int index = router.route("group.topic" + i)
            distribution[index]++
        }

        then:
        int expectedPerMember = topicCount / poolSize
        (0..<poolSize).every { distribution[it] == expectedPerMember }
    }

    def "should be thread-safe and assign each topic to exactly one index"() {
        given:
        int poolSize = 4
        int topicCount = 1000
        int threadCount = 8
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(poolSize)
        ConcurrentHashMap<String, Set<Integer>> topicResults = new ConcurrentHashMap<>()
        CountDownLatch startLatch = new CountDownLatch(1)
        CountDownLatch doneLatch = new CountDownLatch(threadCount)
        def executor = Executors.newFixedThreadPool(threadCount)

        when:
        (0..<threadCount).each { t ->
            executor.submit {
                startLatch.await()
                (0..<topicCount).each { i ->
                    String topic = "group.topic" + i
                    int index = router.route(topic)
                    topicResults.computeIfAbsent(topic, { new ConcurrentHashMap<>().newKeySet() }).add(index)
                }
                doneLatch.countDown()
            }
        }
        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        then: "each topic should have been assigned to exactly one index"
        topicResults.every { topic, indices -> indices.size() == 1 }

        and: "all indices should be within bounds"
        topicResults.every { topic, indices -> indices.first() >= 0 && indices.first() < poolSize }

        and: "distribution should be even"
        int[] distribution = new int[poolSize]
        topicResults.each { topic, indices -> distribution[indices.first()]++ }
        int expectedPerMember = topicCount / poolSize
        (0..<poolSize).every { distribution[it] == expectedPerMember }
    }

    def "getDistribution should return all zeros for fresh router"() {
        given:
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(3)

        expect:
        router.getDistribution() == [0, 0, 0] as int[]
    }

    def "getDistribution should reflect assigned topics"() {
        given:
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(3)

        when:
        router.route("topic.a")
        router.route("topic.b")
        router.route("topic.c")
        router.route("topic.d")
        router.route("topic.e")

        then:
        router.getDistribution() == [2, 2, 1] as int[]
    }

    def "getDistribution should not change when same topic is routed again"() {
        given:
        KafkaMessageSenderPoolRouter router = new KafkaMessageSenderPoolRouter(2)

        when:
        router.route("topic.a")
        router.route("topic.a")
        router.route("topic.a")

        then:
        router.getDistribution() == [1, 0] as int[]
    }
}
