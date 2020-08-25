package pl.allegro.tech.hermes.management.migration.owner

import pl.allegro.tech.hermes.api.OwnerId
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.management.domain.group.GroupService
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService
import pl.allegro.tech.hermes.management.domain.topic.TopicService
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

import javax.validation.ConstraintViolationException

import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class SupportTeamToOwnerMigratorSpec extends Specification {

    def groupService = Stub(GroupService) {
        listGroups() >> [group("group").withSupportTeam("Team A").build()]
    }
    def topicService = Stub(TopicService)
    def subscriptionService = Stub(SubscriptionService)
    def migrator = new SupportTeamToOwnerMigrator(groupService, topicService, subscriptionService)

    def "should count migrated topics and subcriptions"() {
        given:
        topicService.listTopics("group") >> [createTopic("group.topic")]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.topic")) >> [
                createSubscription("group.topic", "sub")
        ]

        when:
        def stats = migrator.execute("some-source", SupportTeamToOwnerMigrator.OwnerExistsStrategy.SKIP)

        then:
        stats.topics().migrated() == 1
        stats.topics().skipped().isEmpty()
        stats.subscriptions().migrated() == 1
        stats.subscriptions().skipped().isEmpty()
    }

    def "should skip topics and subscriptions that already have an owner when using SKIP strategy"() {
        given:
        topicService.listTopics("group") >> [topic("group.topic").withOwner(new OwnerId("a", "b")).build()]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.topic")) >> [
                subscription(TopicName.fromQualifiedName("group.topic"), "sub").withOwner(new OwnerId("a", "b")).withSupportTeam("Alpha").build()
        ]

        when:
        def stats = migrator.execute("some-source", SupportTeamToOwnerMigrator.OwnerExistsStrategy.SKIP)

        then:
        stats.topics().migrated() == 0
        stats.topics().skipped() == ["owner already exists": 1]
        stats.subscriptions().migrated() == 0
        stats.subscriptions().skipped() == ["owner already exists": 1]
    }

    def "should override owners for topics and subscriptions that already have an owner when using OVERRIDE strategy"() {
        given:
        topicService.listTopics("group") >> [topic("group.topic").withOwner(new OwnerId("a", "b")).build()]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.topic")) >> [
                subscription(TopicName.fromQualifiedName("group.topic"), "sub").withOwner(new OwnerId("a", "b")).withSupportTeam("Alpha").build()
        ]

        when:
        def stats = migrator.execute("some-source", SupportTeamToOwnerMigrator.OwnerExistsStrategy.OVERRIDE)

        then:
        stats.topics().migrated() == 1
        stats.topics().skipped().isEmpty()
        stats.subscriptions().migrated() == 1
        stats.subscriptions().skipped().isEmpty()
    }

    def "should skip topics and subscriptions that failed to get updated"() {
        given:
        topicService.listTopics("group") >> [
                createTopic("group.invalid"),
                createTopic("group.error"),
                createTopic("group.success")
        ]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.invalid")) >> [
                createSubscription("group.invalid", "success")
        ]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.error")) >> [
                createSubscription("group.error", "success")
        ]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.success")) >> [
                createSubscription("group.success", "invalid"),
                createSubscription("group.success", "error"),
                createSubscription("group.success", "success")
        ]

        topicService.updateTopic({ it.getName().contains("invalid") }, _, _) >> {
            throw new ConstraintViolationException("invalid", Collections.emptySet())
        }
        topicService.updateTopic({ it.getName().contains("error") }, _, _) >> {
            throw new RuntimeException("error")
        }
        subscriptionService.updateSubscription(_, { it.contains("invalid") }, _, _) >> {
            throw new ConstraintViolationException("invalid", Collections.emptySet())
        }
        subscriptionService.updateSubscription(_, { it.contains("error") }, _, _) >> {
            throw new RuntimeException("error")
        }

        when:
        def stats = migrator.execute("some-source", SupportTeamToOwnerMigrator.OwnerExistsStrategy.SKIP)

        then:
        stats.topics().migrated() == 1
        stats.topics().skipped() == ["javax.validation.ConstraintViolationException": 1, "java.lang.RuntimeException": 1]
        stats.subscriptions().migrated() == 3
        stats.subscriptions().skipped() == ["javax.validation.ConstraintViolationException": 1, "java.lang.RuntimeException": 1]
    }

    private Topic createTopic(String topic) {
        TopicBuilder.topic(topic).withOwner(null).build()
    }

    private Subscription createSubscription(String topic, String subscription) {
        SubscriptionBuilder.subscription(TopicName.fromQualifiedName(topic), subscription).withOwner(null).withSupportTeam("Alpha").build()
    }

}
