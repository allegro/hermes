package pl.allegro.tech.hermes.management.domain.topic;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.TopicNameWithMetrics;
import pl.allegro.tech.hermes.api.TopicStats;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.api.helpers.Patch;
import pl.allegro.tech.hermes.common.logging.LoggingContext;
import pl.allegro.tech.hermes.domain.topic.TopicAlreadyExistsException;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreview;
import pl.allegro.tech.hermes.domain.topic.preview.MessagePreviewRepository;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.dc.DatacenterBoundRepositoryHolder;
import pl.allegro.tech.hermes.management.domain.dc.MultiDatacenterRepositoryCommandExecutor;
import pl.allegro.tech.hermes.management.domain.dc.RepositoryManager;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionRemover;
import pl.allegro.tech.hermes.management.domain.topic.commands.CreateTopicRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.topic.commands.RemoveTopicRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.topic.commands.TouchTopicRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.topic.commands.UpdateTopicRepositoryCommand;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaService;
import pl.allegro.tech.hermes.management.domain.topic.validator.TopicValidator;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

public class TopicService implements TopicManagement {

  private static final Logger logger = LoggerFactory.getLogger(TopicService.class);

  private final TopicRepository topicRepository;
  private final GroupService groupService;
  private final TopicParameters topicParameters;
  private final SchemaService schemaService;

  private final TopicMetricsRepository metricRepository;
  private final MultiDCAwareService multiDCAwareService;
  private final TopicValidator topicValidator;
  private final TopicContentTypeMigrationService topicContentTypeMigrationService;
  private final Clock clock;
  private final Auditor auditor;
  private final MultiDatacenterRepositoryCommandExecutor multiDcExecutor;
  private final RepositoryManager repositoryManager;
  private final TopicOwnerCache topicOwnerCache;
  private final SubscriptionRemover subscriptionRemover;
  private final ScheduledExecutorService scheduledTopicExecutor =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("scheduled-topic-executor-%d").build());

  public TopicService(
      MultiDCAwareService multiDCAwareService,
      TopicRepository topicRepository,
      GroupService groupService,
      TopicParameters topicParameters,
      SchemaService schemaService,
      TopicMetricsRepository metricRepository,
      TopicValidator topicValidator,
      TopicContentTypeMigrationService topicContentTypeMigrationService,
      Clock clock,
      Auditor auditor,
      MultiDatacenterRepositoryCommandExecutor multiDcExecutor,
      RepositoryManager repositoryManager,
      TopicOwnerCache topicOwnerCache,
      SubscriptionRemover subscriptionRemover) {
    this.multiDCAwareService = multiDCAwareService;
    this.topicRepository = topicRepository;
    this.groupService = groupService;
    this.topicParameters = topicParameters;
    this.schemaService = schemaService;
    this.metricRepository = metricRepository;
    this.topicValidator = topicValidator;
    this.topicContentTypeMigrationService = topicContentTypeMigrationService;
    this.clock = clock;
    this.auditor = auditor;
    this.multiDcExecutor = multiDcExecutor;
    this.repositoryManager = repositoryManager;
    this.topicOwnerCache = topicOwnerCache;
    this.subscriptionRemover = subscriptionRemover;
  }

  @Override
  public void createTopicWithSchema(
      TopicWithSchema topicWithSchema, RequestUser createdBy, CreatorRights isAllowedToManage) {
    Topic topic = topicWithSchema.getTopic();
    auditor.beforeObjectCreation(createdBy.getUsername(), topic);
    groupService.checkGroupExists(topic.getName().getGroupName());
    topicValidator.ensureCreatedTopicIsValid(topic, createdBy, isAllowedToManage);
    ensureTopicDoesNotExist(topic);

    boolean validateAndRegisterSchema =
        AVRO.equals(topic.getContentType())
            || (topic.isJsonToAvroDryRunEnabled() && topicWithSchema.getSchema() != null);

    validateSchema(validateAndRegisterSchema, topicWithSchema, topic);
    registerAvroSchema(validateAndRegisterSchema, topicWithSchema, createdBy);
    createTopic(topic, createdBy, isAllowedToManage);
  }

  @Override
  public void removeTopicWithSchema(Topic topic, RequestUser removedBy) {
    auditor.beforeObjectRemoval(
        removedBy.getUsername(), Topic.class.getSimpleName(), topic.getQualifiedName());
    subscriptionRemover.removeSubscriptionRelatedToTopic(topic, removedBy);
    removeSchema(topic);
    if (!topicParameters.isAllowRemoval()) {
      throw new TopicRemovalDisabledException(topic);
    }
    removeTopic(topic, removedBy);
  }

  @Override
  public void updateTopicWithSchema(TopicName topicName, PatchData patch, RequestUser modifiedBy) {
    Topic topic = getTopicDetails(topicName);
    extractSchema(patch)
        .ifPresent(
            schema -> {
              schemaService.registerSchema(topic, schema);
              scheduleTouchTopic(topicName, modifiedBy);
            });
    updateTopic(topicName, patch, modifiedBy);
  }

  private void updateTopic(TopicName topicName, PatchData patch, RequestUser modifiedBy) {
    auditor.beforeObjectUpdate(
        modifiedBy.getUsername(), Topic.class.getSimpleName(), topicName, patch);
    groupService.checkGroupExists(topicName.getGroupName());

    Topic retrieved = getTopicDetails(topicName);
    Topic modified = Patch.apply(retrieved, patch);

    topicValidator.ensureUpdatedTopicIsValid(modified, retrieved, modifiedBy);

    if (!retrieved.equals(modified)) {
      Instant beforeMigrationInstant = clock.instant();
      // TODO: this does not work as intended, uses == instead of equals
      if (retrieved.getRetentionTime() != modified.getRetentionTime()) {
        multiDCAwareService.manageTopic(
            brokerTopicManagement -> brokerTopicManagement.updateTopic(modified));
      }
      multiDcExecutor.executeByUser(new UpdateTopicRepositoryCommand(modified), modifiedBy);

      if (!retrieved.wasMigratedFromJsonType() && modified.wasMigratedFromJsonType()) {
        logger.info(
            "Waiting until all subscriptions have consumers assigned during topic {} content type migration...",
            topicName.qualifiedName());
        topicContentTypeMigrationService.waitUntilAllSubscriptionsHasConsumersAssigned(
            modified,
            Duration.ofSeconds(
                topicParameters.getSubscriptionsAssignmentsCompletedTimeoutSeconds()));
        logger.info(
            "Notifying subscriptions' consumers about changes in topic {} content type...",
            topicName.qualifiedName());
        topicContentTypeMigrationService.notifySubscriptions(
            modified, beforeMigrationInstant, modifiedBy);
      }
      auditor.objectUpdated(modifiedBy.getUsername(), retrieved, modified);
      topicOwnerCache.onUpdatedTopic(retrieved, modified);
    }
  }

  /**
   * Topic is touched so other Hermes instances are notified to read latest topic schema from
   * schema-registry. However, schema-registry can be distributed so when schema is written there
   * then it can not be available on all nodes immediately. This is the reason why we delay touch of
   * topic here, to wait until schema is distributed on schema-registry nodes.
   */
  @Override
  public void scheduleTouchTopic(TopicName topicName, RequestUser touchedBy) {
    if (topicParameters.isTouchSchedulerEnabled()) {
      logger.info("Scheduling touch of topic {}", topicName.qualifiedName());
      scheduledTopicExecutor.schedule(
          () -> touchTopicWithLogging(topicName, touchedBy),
          topicParameters.getTouchDelayInSeconds(),
          TimeUnit.SECONDS);
    } else {
      touchTopic(topicName, touchedBy);
    }
  }

  private void touchTopicWithLogging(TopicName topicName, RequestUser touchedBy) {
    LoggingContext.runWithLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> touchTopic(topicName, touchedBy));
  }

  private void touchTopic(TopicName topicName, RequestUser touchedBy) {
    logger.info("Touching topic {}", topicName.qualifiedName());
    multiDcExecutor.executeByUser(new TouchTopicRepositoryCommand(topicName), touchedBy);
  }

  @Override
  public List<String> listQualifiedTopicNames(String groupName) {
    return topicRepository.listTopicNames(groupName).stream()
        .map(topicName -> new TopicName(groupName, topicName).qualifiedName())
        .collect(toList());
  }

  @Override
  public List<String> listQualifiedTopicNames() {
    return groupService.listGroupNames().stream()
        .map(this::listQualifiedTopicNames)
        .flatMap(List::stream)
        .sorted()
        .collect(toList());
  }

  @Override
  public List<Topic> listTopics(String groupName) {
    return topicRepository.listTopics(groupName);
  }

  @Override
  public Topic getTopicDetails(TopicName topicName) {
    return topicRepository.getTopicDetails(topicName);
  }

  @Override
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

  @Override
  public TopicMetrics getTopicMetrics(TopicName topicName) {
    return topicRepository.topicExists(topicName)
        ? metricRepository.loadMetrics(topicName)
        : TopicMetrics.unavailable();
  }

  @Override
  public String fetchSingleMessageFromPrimary(
      String brokersClusterName, TopicName topicName, Integer partition, Long offset) {
    return multiDCAwareService.readMessageFromPrimary(
        brokersClusterName, getTopicDetails(topicName), partition, offset);
  }

  @Override
  public List<String> listTrackedTopicNames() {
    return groupService.listGroupNames().stream()
        .map(topicRepository::listTopics)
        .flatMap(List::stream)
        .filter(Topic::isTrackingEnabled)
        .map(Topic::getQualifiedName)
        .collect(toList());
  }

  @Override
  public List<String> listTrackedTopicNames(String groupName) {
    return listTopics(groupName).stream()
        .filter(Topic::isTrackingEnabled)
        .map(Topic::getQualifiedName)
        .collect(toList());
  }

  @Override
  public List<String> listFilteredTopicNames(Query<Topic> query) {
    return queryTopic(query).stream().map(Topic::getQualifiedName).collect(toList());
  }

  @Override
  public List<String> listFilteredTopicNames(String groupName, Query<Topic> query) {
    return query.filter(listTopics(groupName)).map(Topic::getQualifiedName).collect(toList());
  }

  @Override
  public List<Topic> queryTopic(Query<Topic> query) {
    return query.filter(getAllTopics()).collect(toList());
  }

  @Override
  public List<Topic> getAllTopics() {
    return topicRepository.listAllTopics();
  }

  @Override
  public Optional<byte[]> preview(TopicName topicName, int idx) {
    List<byte[]> result =
        loadMessagePreviewsFromAllDc(topicName).stream().map(MessagePreview::getContent).toList();

    if (idx >= 0 && idx < result.size()) {
      return Optional.of(result.get(idx));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public List<MessageTextPreview> previewText(TopicName topicName) {
    return loadMessagePreviewsFromAllDc(topicName).stream()
        .map(
            p ->
                new MessageTextPreview(
                    new String(p.getContent(), StandardCharsets.UTF_8), p.isTruncated()))
        .collect(toList());
  }

  @Override
  public List<TopicNameWithMetrics> queryTopicsMetrics(Query<TopicNameWithMetrics> query) {
    List<Topic> filteredNames = query.filterNames(getAllTopics()).collect(toList());
    return query.filter(getTopicsMetrics(filteredNames)).collect(toList());
  }

  @Override
  public TopicStats getStats() {
    List<Topic> topics = getAllTopics();
    long ackAllTopicCount = topics.stream().filter(t -> t.getAck() == Topic.Ack.ALL).count();
    long trackingEnabledTopicCount = topics.stream().filter(Topic::isTrackingEnabled).count();
    long avroTopicCount = topics.stream().filter(t -> t.getContentType() == AVRO).count();
    return new TopicStats(
        topics.size(), ackAllTopicCount, trackingEnabledTopicCount, avroTopicCount);
  }

  private void ensureTopicDoesNotExist(Topic topic) {
    if (topicRepository.topicExists(topic.getName())) {
      throw new TopicAlreadyExistsException(topic.getName());
    }
  }

  private void validateSchema(
      boolean shouldValidate, TopicWithSchema topicWithSchema, Topic topic) {
    if (shouldValidate) {
      schemaService.validateSchema(topic, topicWithSchema.getSchema());
      boolean schemaAlreadyRegistered =
          schemaService.getSchema(topic.getQualifiedName()).isPresent();
      if (schemaAlreadyRegistered) {
        throw new TopicSchemaExistsException(topic.getQualifiedName());
      }
    }
  }

  private void registerAvroSchema(
      boolean shouldRegister, TopicWithSchema topicWithSchema, RequestUser createdBy) {
    if (shouldRegister) {
      try {
        schemaService.registerSchema(topicWithSchema.getTopic(), topicWithSchema.getSchema());
      } catch (Exception e) {
        logger.error(
            "Rolling back topic {} creation due to schema registration error",
            topicWithSchema.getQualifiedName(),
            e);
        removeTopic(topicWithSchema.getTopic(), createdBy);
        throw e;
      }
    }
  }

  private void createTopic(Topic topic, RequestUser createdBy, CreatorRights creatorRights) {
    topicValidator.ensureCreatedTopicIsValid(topic, createdBy, creatorRights);

    if (!multiDCAwareService.topicExists(topic)) {
      createTopicInBrokers(topic, createdBy);
      auditor.objectCreated(createdBy.getUsername(), topic);
      topicOwnerCache.onCreatedTopic(topic);
    } else {
      logger.info(
          "Skipping creation of topic {} on brokers, topic already exists",
          topic.getQualifiedName());
    }

    multiDcExecutor.executeByUser(new CreateTopicRepositoryCommand(topic), createdBy);
  }

  private void createTopicInBrokers(Topic topic, RequestUser createdBy) {
    try {
      multiDCAwareService.manageTopic(
          brokerTopicManagement -> brokerTopicManagement.createTopic(topic));
    } catch (Exception exception) {
      logger.error(
          String.format(
              "Could not create topic %s, rollback topic creation.", topic.getQualifiedName()),
          exception);
      multiDcExecutor.executeByUser(new RemoveTopicRepositoryCommand(topic.getName()), createdBy);
    }
  }

  private void removeSchema(Topic topic) {
    if (AVRO.equals(topic.getContentType()) && topicParameters.isRemoveSchema()) {
      schemaService
          .getSchema(topic.getQualifiedName())
          .ifPresent(s -> schemaService.deleteAllSchemaVersions(topic.getQualifiedName()));
    }
  }

  private void removeTopic(Topic topic, RequestUser removedBy) {
    logger.info("Removing topic: {} from ZK clusters", topic.getQualifiedName());
    long start = System.currentTimeMillis();
    multiDcExecutor.executeByUser(new RemoveTopicRepositoryCommand(topic.getName()), removedBy);
    logger.info(
        "Removed topic: {} from ZK clusters in: {} ms",
        topic.getQualifiedName(),
        System.currentTimeMillis() - start);
    logger.info("Removing topic: {} from Kafka clusters", topic.getQualifiedName());
    start = System.currentTimeMillis();
    multiDCAwareService.manageTopic(
        brokerTopicManagement -> brokerTopicManagement.removeTopic(topic));
    logger.info(
        "Removed topic: {} from Kafka clusters in: {} ms",
        topic.getQualifiedName(),
        System.currentTimeMillis() - start);
    auditor.objectRemoved(removedBy.getUsername(), topic);
    topicOwnerCache.onRemovedTopic(topic);
  }

  private Optional<String> extractSchema(PatchData patch) {
    return Optional.ofNullable(patch.patch().get("schema")).map(o -> (String) o);
  }

  private List<MessagePreview> loadMessagePreviewsFromAllDc(TopicName topicName) {
    List<DatacenterBoundRepositoryHolder<MessagePreviewRepository>> repositories =
        repositoryManager.getRepositories(MessagePreviewRepository.class);
    List<MessagePreview> previews = new ArrayList<>();
    for (DatacenterBoundRepositoryHolder<MessagePreviewRepository> holder : repositories) {
      try {
        previews.addAll(holder.getRepository().loadPreview(topicName));
      } catch (Exception e) {
        logger.warn("Could not load message preview for DC: {}", holder.getDatacenterName());
      }
    }
    return previews;
  }

  private List<TopicNameWithMetrics> getTopicsMetrics(List<Topic> topics) {
    return topics.stream()
        .map(
            t -> {
              TopicMetrics metrics = metricRepository.loadMetrics(t.getName());
              return TopicNameWithMetrics.from(metrics, t.getQualifiedName());
            })
        .collect(toList());
  }

  @Override
  public List<Topic> listForOwnerId(OwnerId ownerId) {
    Collection<TopicName> topicNames = topicOwnerCache.get(ownerId);
    return topicRepository.getTopicsDetails(topicNames);
  }
}
