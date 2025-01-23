package pl.allegro.tech.hermes.management.domain.subscription.commands

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.subscription.SubscriptionAlreadyExistsException
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder
import spock.lang.Shared
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class CreateSubscriptionRepositoryCommandTest extends Specification {

    @Shared
    def topicName = new TopicName("group", "topic")

    @Shared
    def subscriptionName = "subscription"

    @Shared
    Subscription subscription = subscription(topicName, subscriptionName).build()

    def "should not remove subscription if subscription already exists during rollback"() {
        given:
        SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
        DatacenterBoundRepositoryHolder<SubscriptionRepository> repository = Mock(DatacenterBoundRepositoryHolder) {
            getRepository() >> subscriptionRepository
        }
        def command = new CreateSubscriptionRepositoryCommand(subscription)

        when:
        command.rollback(repository, new SubscriptionAlreadyExistsException(subscription))

        then:
        0 * subscriptionRepository.removeSubscription(topicName, subscriptionName)
    }

    def "should remove subscription during rollback"() {
        given:
        SubscriptionRepository subscriptionRepository = Mock(SubscriptionRepository)
        DatacenterBoundRepositoryHolder<SubscriptionRepository> repository = Mock(DatacenterBoundRepositoryHolder) {
            getRepository() >> subscriptionRepository
        }
        def command = new CreateSubscriptionRepositoryCommand(subscription)

        when:
        command.rollback(repository, new RuntimeException())

        then:
        1 * subscriptionRepository.removeSubscription(topicName, subscriptionName)
    }
}
