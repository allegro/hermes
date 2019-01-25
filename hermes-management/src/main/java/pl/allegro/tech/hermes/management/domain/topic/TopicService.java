package pl.allegro.tech.hermes.management.domain.topic;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.TopicNameWithMetrics;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.domain.topic.TopicAlreadyExistsException;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaService;
import pl.allegro.tech.hermes.management.domain.topic.validator.TopicValidator;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;

@Component
public class TopicService {

    private static final Logger logger = LoggerFactory.getLogger(TopicService.class);

    private final TopicRepository topicRepository;
    private final GroupService groupService;
    private final TopicProperties topicProperties;
    private final SchemaService schemaService;

    private final TopicMetricsRepository metricRepository;
    private final MessagePreviewRepository messagePreviewRepository;
    private final MultiDCAwareService multiDCAwareService;
    private final TopicValidator topicValidator;
    private final TopicContentTypeMigrationService topicContentTypeMigrationService;
    private final Clock clock;
    private final Auditor auditor;
    private final TopicOwnerCache topicOwnerCache;
    private final ScheduledExecutorService scheduledTopicExecutor = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder()
                    .setNameFormat("scheduled-topic-executor-%d")
                    .build());

    @Autowired
    public TopicService(MultiDCAwareService multiDCAwareService,
                        TopicRepository topicRepository,
                        GroupService groupService,
                        TopicProperties topicProperties,
                        SchemaService schemaService, TopicMetricsRepository metricRepository,
                        TopicValidator topicValidator,
                        TopicContentTypeMigrationService topicContentTypeMigrationService,
                        MessagePreviewRepository messagePreviewRepository,
                        Clock clock,
                        Auditor auditor,
                        TopicOwnerCache topicOwnerCache) {
        this.multiDCAwareService = multiDCAwareService;
        this.topicRepository = topicRepository;
        this.groupService = groupService;
        this.topicProperties = topicProperties;
        this.schemaService = schemaService;
        this.metricRepository = metricRepository;
        this.topicValidator = topicValidator;
        this.topicContentTypeMigrationService = topicContentTypeMigrationService;
        this.messagePreviewRepository = messagePreviewRepository;
        this.clock = clock;
        this.auditor = auditor;
        this.topicOwnerCache = topicOwnerCache;
    }

    public void createTopicWithSchema(TopicWithSchema topicWithSchema, String createdBy, CreatorRights isAllowedToManage) {
        Topic topic = topicWithSchema.getTopic();
        topicValidator.ensureCreatedTopicIsValid(topic, isAllowedToManage);
        ensureTopicDoesNotExist(topic);

        boolean validateAndRegisterSchema = AVRO.equals(topic.getContentType()) || (topic.isJsonToAvroDryRunEnabled()
                && topicWithSchema.getSchema() != null);

        validateSchema(validateAndRegisterSchema, topicWithSchema, topic);
        registerAvroSchema(validateAndRegisterSchema, topicWithSchema, createdBy);
        createTopic(topic, createdBy);
    }

    private void ensureTopicDoesNotExist(Topic topic) {
        if (topicRepository.topicExists(topic.getName())) {
            throw new TopicAlreadyExistsException(topic.getName());
        }
    }

    private void validateSchema(boolean shouldValidate, TopicWithSchema topicWithSchema, Topic topic) {
        if (shouldValidate) {
            schemaService.validateSchema(topic.getName(), topicWithSchema.getSchema());
            boolean schemaAlreadyRegistered = schemaService.getSchema(topic.getQualifiedName()).isPresent();
            if (schemaAlreadyRegistered) {
                throw new TopicSchemaExistsException(topic.getQualifiedName());
            }
        }
    }

    private void registerAvroSchema(boolean shouldRegister, TopicWithSchema topicWithSchema, String createdBy) {
        if (shouldRegister) {
            try {
                schemaService.registerSchema(topicWithSchema.getTopic(), topicWithSchema.getSchema(), true);
            } catch (Exception e) {
                logger.error("Rolling back topic {} creation due to schema registration error", topicWithSchema.getQualifiedName(), e);
                removeTopic(topicWithSchema.getTopic(), createdBy);
                throw e;
            }
        }
    }

    private void createTopic(Topic topic, String createdBy) {
        topicRepository.createTopic(topic);

        if (!multiDCAwareService.topicExists(topic)) {
            createTopicInBrokers(topic);
            auditor.objectCreated(createdBy, topic);
            topicOwnerCache.onCreatedTopic(topic);
        } else {
            logger.info("Skipping creation of topic {} on brokers, topic already exists", topic.getQualifiedName());
        }
    }

    private void createTopicInBrokers(Topic topic) {
        try {
            multiDCAwareService.manageTopic(brokerTopicManagement ->
                    brokerTopicManagement.createTopic(topic)
            );
        } catch (Exception exception) {
            logger.error(
                    String.format("Could not create topic %s, rollback topic creation.", topic.getQualifiedName()),
                    exception
            );
            topicRepository.removeTopic(topic.getName());
        }
    }

    public void removeTopicWithSchema(Topic topic, String removedBy) {
        removeSchema(topic);
        if (!topicProperties.isAllowRemoval()) {
            throw new TopicRemovalDisabledException(topic);
        }
        removeTopic(topic, removedBy);
    }

    private void removeSchema(Topic topic) {
        if (AVRO.equals(topic.getContentType()) && topicProperties.isRemoveSchema()) {
            schemaService.getSchema(topic.getQualifiedName()).ifPresent(s ->
                    schemaService.deleteAllSchemaVersions(topic.getQualifiedName()));
        }
    }

    private void removeTopic(Topic topic, String removedBy) {
        topicRepository.removeTopic(topic.getName());
        multiDCAwareService.manageTopic(brokerTopicManagement -> brokerTopicManagement.removeTopic(topic));
        auditor.objectRemoved(removedBy, Topic.class.getSimpleName(), topic.getQualifiedName());
        topicOwnerCache.onRemovedTopic(topic);
    }

    public void updateTopicWithSchema(TopicName topicName, PatchData patch, String modifiedBy) {
        Topic topic = getTopicDetails(topicName);
        boolean validateAvroSchema = AVRO.equals(topic.getContentType());
        extractSchema(patch)
                .ifPresent(schema -> {
                    schemaService.registerSchema(topic, schema, validateAvroSchema);
                    scheduleTouchTopic(topicName);
                });
        updateTopic(topicName, patch, modifiedBy);
    }

    private Optional<String> extractSchema(PatchData patch) {
        return Optional.ofNullable(patch.getPatch().get("schema")).map(o -> (String) o);
    }

    public void updateTopic(TopicName topicName, PatchData patch, String modifiedBy) {
        groupService.checkGroupExists(topicName.getGroupName());

        Topic retrieved = getTopicDetails(topicName);
        Topic modified = Patch.apply(retrieved, patch);

        topicValidator.ensureUpdatedTopicIsValid(modified, retrieved);

        if (!retrieved.equals(modified)) {
            Instant beforeMigrationInstant = clock.instant();
            if (retrieved.getRetentionTime() != modified.getRetentionTime()) {
                multiDCAwareService.manageTopic(brokerTopicManagement ->
                        brokerTopicManagement.updateTopic(modified)
                );
            }
            topicRepository.updateTopic(modified);

            if (!retrieved.wasMigratedFromJsonType() && modified.wasMigratedFromJsonType()) {
                logger.info("Waiting until all subscriptions have consumers assigned during topic {} content type migration...", topicName.qualifiedName());
                topicContentTypeMigrationService.waitUntilAllSubscriptionsHasConsumersAssigned(modified,
                        Duration.ofSeconds(topicProperties.getSubscriptionsAssignmentsCompletedTimeoutSeconds()));
                logger.info("Notifying subscriptions' consumers about changes in topic {} content type...", topicName.qualifiedName());
                topicContentTypeMigrationService.notifySubscriptions(modified, beforeMigrationInstant);
            }
            auditor.objectUpdated(modifiedBy, retrieved, modified);
            topicOwnerCache.onUpdatedTopic(retrieved, modified);
        }
    }

    /**
     * Topic is touched so other Hermes instances are notified to read latest topic schema from schema-registry.
     * However, schema-registry can be distributed so when schema is written there then it can not be available on all nodes immediately.
     * This is the reason why we delay touch of topic here, to wait until schema is distributed on schema-registry nodes.
     */
    public void scheduleTouchTopic(TopicName topicName) {
        if (topicProperties.isTouchSchedulerEnabled()) {
            logger.info("Scheduling touch of topic {}", topicName.qualifiedName());
            scheduledTopicExecutor.schedule(() -> touchTopic(topicName),
                    topicProperties.getTouchDelayInSeconds(),
                    TimeUnit.SECONDS
            );
        } else {
            touchTopic(topicName);
        }
    }

    private void touchTopic(TopicName topicName) {
        logger.info("Touching topic {}", topicName.qualifiedName());
        topicRepository.touchTopic(topicName);
    }

    public List<String> listQualifiedTopicNames(String groupName) {
        return topicRepository.listTopicNames(groupName).stream()
                .map(topicName -> new TopicName(groupName, topicName).qualifiedName())
                .collect(toList());
    }

    public List<Topic> listTopics(String groupName) {
        return topicRepository.listTopics(groupName);
    }

    public List<String> listQualifiedTopicNames() {
        return groupService.listGroupNames().stream()
                .map(this::listQualifiedTopicNames)
                .flatMap(List::stream)
                .sorted()
                .collect(toList());
    }

    public Topic getTopicDetails(TopicName topicName) {
        return topicRepository.getTopicDetails(topicName);
    }

    public TopicWithSchema getTopicWithSchema(TopicName topicName) {
        Topic topic = getTopicDetails(topicName);
        Optional<RawSchema> schema = Optional.empty();
        if (AVRO.equals(topic.getContentType())) {
            schema = schemaService.getSchema(topicName.qualifiedName());
        }
        return schema
                .map(s -> topicWithSchema(topic, s.value()))
                .orElseGet(() -> topicWithSchema(topic));
    }

    public TopicMetrics getTopicMetrics(TopicName topicName) {
        return topicRepository.topicExists(topicName) ? metricRepository.loadMetrics(topicName) : TopicMetrics.unavailable();
    }

    public String fetchSingleMessageFromPrimary(String brokersClusterName, TopicName topicName, Integer partition, Long offset) {
        return multiDCAwareService.readMessageFromPrimary(brokersClusterName, getTopicDetails(topicName), partition, offset);
    }

    public List<String> listTrackedTopicNames() {
        return groupService.listGroupNames().stream()
                .map(topicRepository::listTopics)
                .flatMap(List::stream)
                .filter(Topic::isTrackingEnabled)
                .map(Topic::getQualifiedName)
                .collect(toList());
    }

    public List<String> listTrackedTopicNames(String groupName) {
        return listTopics(groupName).stream()
                .filter(Topic::isTrackingEnabled)
                .map(Topic::getQualifiedName)
                .collect(toList());
    }

    public List<String> listFilteredTopicNames(Query<Topic> query) {
        return queryTopic(query)
                .stream()
                .map(Topic::getQualifiedName)
                .collect(toList());
    }

    public List<String> listFilteredTopicNames(String groupName, Query<Topic> query) {
        return query.filter(listTopics(groupName))
                .map(Topic::getQualifiedName)
                .collect(toList());
    }

    public List<Topic> queryTopic(Query<Topic> query) {
        return query
                .filter(getAllTopics())
                .collect(toList());
    }

    public List<Topic> getAllTopics() {
        return groupService
                .listGroupNames()
                .stream()
                .map(topicRepository::listTopics)
                .flatMap(List::stream)
                .collect(toList());
    }

    public Optional<byte[]> preview(TopicName topicName, int idx) {
        List<byte[]> result = messagePreviewRepository.loadPreview(topicName)
                .stream()
                .map(MessagePreview::getContent)
                .collect(toList());

        if (idx >= 0 && idx < result.size()) {
            return Optional.of(result.get(idx));
        } else return Optional.empty();
    }

    public List<MessageTextPreview> previewText(TopicName topicName) {
        return messagePreviewRepository.loadPreview(topicName).stream()
                .map(p -> new MessageTextPreview(new String(p.getContent(), StandardCharsets.UTF_8), p.isTruncated()))
                .collect(toList());
    }

    public List<TopicNameWithMetrics> queryTopicsMetrics(Query<TopicNameWithMetrics> query) {
        return query.filter(getTopicsMetrics())
                .collect(toList());
    }

    private List<TopicNameWithMetrics> getTopicsMetrics() {
        return getAllTopics()
                .stream()
                .map(t -> {
                    TopicMetrics metrics = metricRepository.loadMetrics(t.getName());
                    return TopicNameWithMetrics.from(metrics, t.getQualifiedName());
                })
                .collect(toList());
    }

    public List<Topic> listForOwnerId(OwnerId ownerId) {
        Collection<TopicName> topicNames = topicOwnerCache.get(ownerId);
        return topicRepository.getTopicsDetails(topicNames);
    }
}
