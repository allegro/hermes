package pl.allegro.tech.hermes.management.migration.maintainer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.*;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.nullToEmpty;
import static pl.allegro.tech.hermes.api.PatchData.patchData;

@Component
public class SupportTeamToMaintainerMigrator {

    private static final Logger logger = LoggerFactory.getLogger(SupportTeamToMaintainerMigrator.class);

    private static final String MIGRATION_USER = "migration";
    private static final String MAINTAINER_ALREADY_EXISTED_REASON = "maintainer already exists";

    private final GroupService groupService;
    private final TopicService topicService;
    private final SubscriptionService subscriptionService;

    public enum MaintainerExistsStrategy {
        OVERRIDE, SKIP
    }

    @Autowired
    public SupportTeamToMaintainerMigrator(GroupService groupService, TopicService topicService, SubscriptionService subscriptionService) {
        this.groupService = groupService;
        this.topicService = topicService;
        this.subscriptionService = subscriptionService;
    }

    public ExecutionStats execute(String sourceName, MaintainerExistsStrategy strategy) {
        logger.info("Migrating support teams to maintainers...");

        EntityMigrationCounters topicCounters = new EntityMigrationCounters();
        EntityMigrationCounters subscriptionCounters = new EntityMigrationCounters();

        for (Group group : groupService.listGroups()) {
            for (Topic topic : topicService.listTopics(group.getGroupName())) {
                migrateTopic(topic, group, sourceName, strategy, topicCounters);
                for (Subscription subscription : subscriptionService.listSubscriptions(topic.getName())) {
                    migrateSubscription(subscription, sourceName, strategy, subscriptionCounters);
                }
            }
        }

        ExecutionStats stats = new ExecutionStats(topicCounters.toStats(), subscriptionCounters.toStats());
        logger.info("Done migrating support teams to maintainers: {}", stats);
        return stats;
    }

    private void migrateSubscription(Subscription subscription, String sourceName, MaintainerExistsStrategy strategy, EntityMigrationCounters subscriptionCounters) {
        if (subscription.getMaintainer() == null || strategy == MaintainerExistsStrategy.OVERRIDE) {
            migrateEntity(subscriptionCounters, "subscription " + subscription.getQualifiedName(),
                    () -> subscriptionService.updateSubscription(subscription.getTopicName(), subscription.getName(), patchWithMaintainer(sourceName, subscription.getSupportTeam()), MIGRATION_USER)
            );
        } else {
            subscriptionCounters.markSkipped(MAINTAINER_ALREADY_EXISTED_REASON);
        }
    }

    private void migrateTopic(Topic topic, Group group, String sourceName, MaintainerExistsStrategy strategy, EntityMigrationCounters topicCounters) {
        if (topic.getMaintainer() == null || strategy == MaintainerExistsStrategy.OVERRIDE) {
            migrateEntity(topicCounters, "topic " + topic.getQualifiedName(),
                    () -> topicService.updateTopic(topic.getName(), patchWithMaintainer(sourceName, group.getSupportTeam()), MIGRATION_USER)
            );
        } else {
            topicCounters.markSkipped(MAINTAINER_ALREADY_EXISTED_REASON);
        }
    }

    private void migrateEntity(EntityMigrationCounters topicCounters, String humanReadableEntityName, Runnable migration) {
        try {
            migration.run();
            topicCounters.markMigrated();
        } catch (Exception e) {
            logger.info("Failed to migrate {}, skipping", humanReadableEntityName, e);
            topicCounters.markSkipped(e.getClass().getTypeName());
        }
    }

    private PatchData patchWithMaintainer(String sourceName, String supportTeam) {
        return patchData().set("maintainer", new MaintainerDescriptor(sourceName, nullToEmpty(supportTeam))).build();
    }

    private static class EntityMigrationCounters {
        private int migrated = 0;
        private Map<String, Integer> skipped = new HashMap<>();

        void markMigrated() {
            migrated += 1;
        }

        void markSkipped(String reason) {
            skipped.compute(reason, (key, old) -> (old != null ? old : 0) + 1);
        }

        EntityMigrationStats toStats() {
            return new EntityMigrationStats(migrated, skipped);
        }
    }

    public static class ExecutionStats {
        private final EntityMigrationStats topics;
        private final EntityMigrationStats subscriptions;

        @JsonCreator
        public ExecutionStats(@JsonProperty("topics") EntityMigrationStats topics,
                              @JsonProperty("subscriptions") EntityMigrationStats subscriptions) {
            this.topics = topics;
            this.subscriptions = subscriptions;
        }

        @JsonProperty("topics")
        public EntityMigrationStats topics() {
            return topics;
        }

        @JsonProperty("subscriptions")
        public EntityMigrationStats subscriptions() {
            return subscriptions;
        }

        @Override
        public String toString() {
            return "ExecutionStats{" +
                    "topics=" + topics +
                    ", subscriptions=" + subscriptions +
                    '}';
        }
    }

    public static class EntityMigrationStats {
        private final int migrated;
        private final Map<String, Integer> skipped;

        @JsonCreator
        public EntityMigrationStats(@JsonProperty("migrated") int migrated,
                                    @JsonProperty("skipped") Map<String, Integer> skipped) {
            this.migrated = migrated;
            this.skipped = ImmutableMap.copyOf(skipped);
        }

        @JsonProperty("migrated")
        public int migrated() {
            return migrated;
        }

        @JsonProperty("skipped")
        public Map<String, Integer> skipped() {
            return skipped;
        }

        @Override
        public String toString() {
            return "EntityMigrationStats{" +
                    "migrated=" + migrated +
                    ", skipped=" + skipped +
                    '}';
        }
    }

}
