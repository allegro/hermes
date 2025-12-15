package pl.allegro.tech.hermes.management.domain.search

import pl.allegro.tech.hermes.api.OwnerId
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository
import pl.allegro.tech.hermes.domain.topic.TopicRepository
import pl.allegro.tech.hermes.management.domain.search.cache.NotificationBasedSearchCache
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.assertions.SearchResultsAssertion.assertThat

class SearchServiceSpec extends Specification {
    private InternalNotificationsBus notificationsBus
    private TopicRepository topicRepository
    private SubscriptionRepository subscriptionRepository
    private SearchPredicateFactory searchPredicateFactory = new SearchPredicateFactory()
    private NotificationBasedSearchCache searchCache
    private SearchService searchService

    private static final String FIRST_GROUP_NAME = "pl.allegro"
    private static final String FIRST_TOPIC_NAME = "first-topic"
    private static final String FIRST_TOPIC_QUALIFIED_NAME =
            FIRST_GROUP_NAME + "." + FIRST_TOPIC_NAME
    private static final String FIRST_SUBSCRIPTION_NAME = "first-subscription"

    def setup() {
        notificationsBus = Stub()
        topicRepository = Stub()
        subscriptionRepository = Stub()
        searchCache = new NotificationBasedSearchCache(
                notificationsBus,
                topicRepository,
                subscriptionRepository
        )
        searchService = new SearchService(searchCache, searchPredicateFactory)
    }

    def "should return empty results when cache is empty"() {
        given:
        initializeCache(List.of(), List.of())
        def query = new SearchQuery("")

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).hasNoResults()
    }

    def "should return empty results when no matches found"() {
        given:
        def topic = TopicBuilder.topicWithRandomName().build()
        initializeCache(List.of(topic), List.of())
        def query = new SearchQuery("non-existing-query")

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).hasNoResults()
    }

    def "should find single topic"() {
        given:
        def topic = TopicBuilder.topicWithRandomName().build()
        initializeCache(List.of(topic), List.of())
        def query = new SearchQuery(topic.getQualifiedName())

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).containsOnlySingleItemForTopic(
                topic
        )
    }

    def "should only find subscription when only subscription matches"() {
        given:
        def topic = TopicBuilder.topic(FIRST_TOPIC_QUALIFIED_NAME).build()
        def subscription = SubscriptionBuilder.subscription(FIRST_TOPIC_QUALIFIED_NAME, FIRST_SUBSCRIPTION_NAME).build()
        initializeCache(List.of(topic), List.of(subscription))
        def query = new SearchQuery(FIRST_SUBSCRIPTION_NAME)

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).containsOnlySingleItemForSubscription(
                subscription
        )
    }

    def "should find both topic and subscription when both matches query"() {
        given:
        def commonNamePart = "common"
        def topicName = commonNamePart + "-topic"
        def subscriptionName = commonNamePart + "-subscription"
        def topic = TopicBuilder.topic(FIRST_GROUP_NAME, topicName).build()
        def subscription = SubscriptionBuilder.subscription(topic.getQualifiedName(), subscriptionName).build()
        initializeCache(List.of(topic), List.of(subscription))
        def query = new SearchQuery(commonNamePart)

        when:
        def results = searchService.search(query)

        then:
        assertThat(results)
                .hasExactNumberOfResults(2)
                .containsItemForTopic(topic)
                .containsItemForSubscription(subscription)
    }

    def "should find topics by various name queries"() {
        given:
        def topic1 = TopicBuilder.topic("pl.first-topic").build()
        def topic2 = TopicBuilder.topic("pl.second-topic").build()
        def topic3 = TopicBuilder.topic("pl.hermes.third-topic").build()
        initializeCache(List.of(topic1, topic2, topic3), List.of())
        def query = new SearchQuery(queryString)

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).containsExactlyByNameInAnyOrder(expectedTopics)

        where:
        queryString      | expectedTopics
        "topic"          | ["pl.first-topic", "pl.second-topic", "pl.hermes.third-topic"]
        "top"            | ["pl.first-topic", "pl.second-topic", "pl.hermes.third-topic"]
        "first"          | ["pl.first-topic"]
        "second"         | ["pl.second-topic"]
        "third"          | ["pl.hermes.third-topic"]
        "hermes"         | ["pl.hermes.third-topic"]
        "PL.FIRST-TOPIC" | ["pl.first-topic"]
        "non-existing"   | []
    }

    def "should find topics by various owner id queries"() {
        given:
        def topic1 =
                TopicBuilder.topic("pl.first-topic").withOwner(new OwnerId("Plaintext", "1234")).build()
        def topic2 =
                TopicBuilder.topic("pl.second-topic").withOwner(new OwnerId("Plaintext", "3456")).build()
        initializeCache(List.of(topic1, topic2), List.of())
        def query = new SearchQuery(queryString)

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).containsExactlyByNameInAnyOrder(expectedTopics)

        where:
        queryString | expectedTopics
        "1234"      | ["pl.first-topic"]
        "3456"      | ["pl.second-topic"]
        "34"        | ["pl.first-topic", "pl.second-topic"]
        "99"        | []
    }

    def "should find subscriptions by various name queries"() {
        given:
        def subscription1 = SubscriptionBuilder.subscription(
                FIRST_TOPIC_QUALIFIED_NAME,
                "first-subscription"
        ).build()
        def subscription2 = SubscriptionBuilder.subscription(
                FIRST_TOPIC_QUALIFIED_NAME,
                "second-subscription"
        ).build()
        initializeCache(
                List.of(TopicBuilder.topic(FIRST_TOPIC_QUALIFIED_NAME).build()),
                List.of(subscription1, subscription2)
        )
        def query = new SearchQuery(queryString)

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).containsExactlyByNameInAnyOrder(expectedSubscriptions)

        where:
        queryString           | expectedSubscriptions
        "first-subscription"  | ["first-subscription"]
        "first-sub"           | ["first-subscription"]
        "second-subscription" | ["second-subscription"]
        "sub"                 | ["first-subscription", "second-subscription"]
        "SUB"                 | ["first-subscription", "second-subscription"]
        "non-existing"        | []
    }

    def "should find subscriptions by various owner id queries"() {
        given:
        def subscription1 = SubscriptionBuilder.subscription(
                FIRST_TOPIC_QUALIFIED_NAME,
                "first-subscription"
        ).withOwner(new OwnerId("Plaintext", "owner-1234")).build()
        def subscription2 = SubscriptionBuilder.subscription(
                FIRST_TOPIC_QUALIFIED_NAME,
                "second-subscription"
        ).withOwner(new OwnerId("Plaintext", "owner-3456")).build()
        initializeCache(
                List.of(TopicBuilder.topic(FIRST_TOPIC_QUALIFIED_NAME).build()),
                List.of(subscription1, subscription2)
        )
        def query = new SearchQuery(queryString)

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).containsExactlyByNameInAnyOrder(expectedSubscriptions)

        where:
        queryString  | expectedSubscriptions
        "owner-1234" | ["first-subscription"]
        "owner-3456" | ["second-subscription"]
        "owner"      | ["first-subscription", "second-subscription"]
        "owner-99"   | []
    }

    def "should find subscriptions by various endpoint queries"() {
        given:
        def subscription1 = SubscriptionBuilder.subscription(
                FIRST_TOPIC_QUALIFIED_NAME,
                "first-subscription"
        ).withEndpoint("https://localhost/ev/event1").build()
        def subscription2 = SubscriptionBuilder.subscription(
                FIRST_TOPIC_QUALIFIED_NAME,
                "second-subscription"
        ).withEndpoint("https://localhost/ev/event2").build()
        initializeCache(
                List.of(TopicBuilder.topic(FIRST_TOPIC_QUALIFIED_NAME).build()),
                List.of(subscription1, subscription2)
        )
        def query = new SearchQuery(queryString)

        when:
        def results = searchService.search(query)

        then:
        assertThat(results).containsExactlyByNameInAnyOrder(expectedSubscriptions)

        where:
        queryString                   | expectedSubscriptions
        "https://localhost/ev/event1" | ["first-subscription"]
        "https://localhost/ev/event2" | ["second-subscription"]
        "localhost/ev/event1"         | ["first-subscription"]
        "localhost"                   | ["first-subscription", "second-subscription"]
        "localhost/non-existing"      | []
    }

    private def initializeCache(List<Topic> topics, List<Subscription> subscriptions) {
        topicRepository.listAllTopics() >> topics
        subscriptionRepository.listAllSubscriptions() >> subscriptions
        searchCache.initialize()
    }
}
