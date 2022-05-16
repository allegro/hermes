package pl.allegro.tech.hermes.management.migration.owner;

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
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.nullToEmpty;
import static pl.allegro.tech.hermes.api.PatchData.patchData;

@Component
public class SupportTeamToOwnerMigrator {

    private static final Logger logger = LoggerFactory.getLogger(SupportTeamToOwnerMigrator.class);

    private static final String MIGRATION_USER = "migration";
    private static final String OWNER_ALREADY_EXISTED_REASON = "owner already exists";

    private final GroupService groupService;
    private final TopicService topicService;
    private final SubscriptionService subscriptionService;

    public enum OwnerExistsStrategy {
        OVERRIDE, SKIP
    }

    @Autowired
    public SupportTeamToOwnerMigrator(GroupService groupService, TopicService topicService, SubscriptionService subscriptionService) {
        this.groupService = groupService;
        this.topicService = topicService;
        this.subscriptionService = subscriptionService;
    }

    public ExecutionStats execute(String sourceName, OwnerExistsStrategy strategy) {
        logger.info("Migrating support teams to owners...");

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
        logger.info("Done migrating support teams to owners: {}", stats);
        return stats;
    }

    private void migrateSubscription(Subscription subscription, String sourceName, OwnerExistsStrategy strategy, EntityMigrationCounters subscriptionCounters) {
        if (subscription.getOwner() == null || strategy == OwnerExistsStrategy.OVERRIDE) {
            migrateEntity(subscriptionCounters, "subscription " + subscription.getQualifiedName(),
                    () -> subscriptionService.updateSubscription(subscription.getTopicName(), subscription.getName(), patchWithOwner(sourceName, subscription.getSupportTeam()), new RequestUser(MIGRATION_USER, false))
            );
        } else {
            subscriptionCounters.markSkipped(OWNER_ALREADY_EXISTED_REASON);
        }
    }

    private void migrateTopic(Topic topic, Group group, String sourceName, OwnerExistsStrategy strategy, EntityMigrationCounters topicCounters) {
        if (topic.getOwner() == null || strategy == OwnerExistsStrategy.OVERRIDE) {
            migrateEntity(topicCounters, "topic " + topic.getQualifiedName(),
                    () -> topicService.updateTopic(topic.getName(), patchWithOwner(sourceName, group.getSupportTeam()), new RequestUser(MIGRATION_USER, true))
            );
        } else {
            topicCounters.markSkipped(OWNER_ALREADY_EXISTED_REASON);
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

    private PatchData patchWithOwner(String sourceName, String supportTeam) {
        return patchData().set("owner", new OwnerId(sourceName, nullToEmpty(supportTeam))).build();
    }

    private static class EntityMigrationCounters {
        private int migrated = 0;
        private final Map<String, Integer> skipped = new HashMap<>();

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
