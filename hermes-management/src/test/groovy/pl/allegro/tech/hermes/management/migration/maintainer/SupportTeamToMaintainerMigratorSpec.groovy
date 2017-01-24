package pl.allegro.tech.hermes.management.migration.maintainer

import pl.allegro.tech.hermes.api.MaintainerDescriptor
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.management.domain.group.GroupService
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService
import pl.allegro.tech.hermes.management.domain.topic.TopicService
import spock.lang.Specification

import javax.validation.ConstraintViolationException

import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class SupportTeamToMaintainerMigratorSpec extends Specification {

    def groupService = Stub(GroupService) {
        listGroups() >> [group("group").withSupportTeam("Team A").build()]
    }
    def topicService = Stub(TopicService)
    def subscriptionService = Stub(SubscriptionService)
    def migrator = new SupportTeamToMaintainerMigrator(groupService, topicService, subscriptionService)

    def "should count migrated topics and subcriptions"() {
        given:
        topicService.listTopics("group") >> [topic("group.topic").withMaintainer(null).build()]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.topic")) >> [
                subscription(TopicName.fromQualifiedName("group.topic"), "sub").withMaintainer(null).withSupportTeam("Alpha").build()
        ]

        when:
        def stats = migrator.execute("some-source", SupportTeamToMaintainerMigrator.MaintainerExistsStrategy.SKIP)

        then:
        stats.topics().migrated() == 1
        stats.topics().skipped().isEmpty()
        stats.subscriptions().migrated() == 1
        stats.subscriptions().skipped().isEmpty()
    }

    def "should skip topics and subscriptions that already have a maintainer when using SKIP strategy"() {
        given:
        topicService.listTopics("group") >> [topic("group.topic").withMaintainer(new MaintainerDescriptor("a", "b")).build()]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.topic")) >> [
                subscription(TopicName.fromQualifiedName("group.topic"), "sub").withMaintainer(new MaintainerDescriptor("a", "b")).withSupportTeam("Alpha").build()
        ]

        when:
        def stats = migrator.execute("some-source", SupportTeamToMaintainerMigrator.MaintainerExistsStrategy.SKIP)

        then:
        stats.topics().migrated() == 0
        stats.topics().skipped() == ["maintainer already exists": 1]
        stats.subscriptions().migrated() == 0
        stats.subscriptions().skipped() == ["maintainer already exists": 1]
    }

    def "should override maintainers for topics and subscriptions that already have a maintainer when using OVERRIDE strategy"() {
        given:
        topicService.listTopics("group") >> [topic("group.topic").withMaintainer(new MaintainerDescriptor("a", "b")).build()]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.topic")) >> [
                subscription(TopicName.fromQualifiedName("group.topic"), "sub").withMaintainer(new MaintainerDescriptor("a", "b")).withSupportTeam("Alpha").build()
        ]

        when:
        def stats = migrator.execute("some-source", SupportTeamToMaintainerMigrator.MaintainerExistsStrategy.OVERRIDE)

        then:
        stats.topics().migrated() == 1
        stats.topics().skipped().isEmpty()
        stats.subscriptions().migrated() == 1
        stats.subscriptions().skipped().isEmpty()
    }

    def "should skip topics and subscriptions that failed to get updated"() {
        given:
        topicService.listTopics("group") >> [
                topic("group.invalid").withMaintainer(null).build(),
                topic("group.error").withMaintainer(null).build(),
                topic("group.success").withMaintainer(null).build()
        ]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.invalid")) >> [
                subscription(TopicName.fromQualifiedName("group.invalid"), "success").withMaintainer(null).withSupportTeam("Alpha").build()
        ]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.error")) >> [
                subscription(TopicName.fromQualifiedName("group.error"), "success").withMaintainer(null).withSupportTeam("Alpha").build()
        ]
        subscriptionService.listSubscriptions(TopicName.fromQualifiedName("group.success")) >> [
                subscription(TopicName.fromQualifiedName("group.success"), "invalid").withMaintainer(null).withSupportTeam("Alpha").build(),
                subscription(TopicName.fromQualifiedName("group.success"), "error").withMaintainer(null).withSupportTeam("Alpha").build(),
                subscription(TopicName.fromQualifiedName("group.success"), "success").withMaintainer(null).withSupportTeam("Alpha").build()
        ]

        topicService.updateTopic({ it.qualifiedName().contains("invalid") }, _, _) >> { throw new ConstraintViolationException("invalid", Collections.emptySet()) }
        topicService.updateTopic({ it.qualifiedName().contains("error") }, _, _) >> { throw new RuntimeException("error") }
        subscriptionService.updateSubscription(_, { it.contains("invalid") }, _, _) >> { throw new ConstraintViolationException("invalid", Collections.emptySet()) }
        subscriptionService.updateSubscription(_, { it.contains("error") }, _, _) >> { throw new RuntimeException("error") }

        when:
        def stats = migrator.execute("some-source", SupportTeamToMaintainerMigrator.MaintainerExistsStrategy.SKIP)

        then:
        stats.topics().migrated() == 1
        stats.topics().skipped() == ["javax.validation.ConstraintViolationException": 1, "java.lang.RuntimeException": 1]
        stats.subscriptions().migrated() == 3
        stats.subscriptions().skipped() == ["javax.validation.ConstraintViolationException": 1, "java.lang.RuntimeException": 1]
    }

}
