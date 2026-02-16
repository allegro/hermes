package pl.allegro.tech.hermes.management.domain.topic;

import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import java.util.List;
import java.util.Optional;
import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.TopicNameWithMetrics;
import pl.allegro.tech.hermes.api.TopicStats;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.common.logging.LoggingContext;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;

public class LoggingTopicService implements TopicManagement {

  private final TopicManagement delegate;

  public LoggingTopicService(TopicManagement delegate) {
    this.delegate = delegate;
  }

  @Override
  public void createTopicWithSchema(
      TopicWithSchema topicWithSchema, RequestUser createdBy, CreatorRights isAllowedToManage) {
    LoggingContext.runWithLogging(
        TOPIC_NAME,
        topicWithSchema.getQualifiedName(),
        () -> delegate.createTopicWithSchema(topicWithSchema, createdBy, isAllowedToManage));
  }

  @Override
  public void removeTopicWithSchema(Topic topic, RequestUser removedBy) {
    LoggingContext.runWithLogging(
        TOPIC_NAME,
        topic.getQualifiedName(),
        () -> delegate.removeTopicWithSchema(topic, removedBy));
  }

  @Override
  public void updateTopicWithSchema(TopicName topicName, PatchData patch, RequestUser modifiedBy) {
    LoggingContext.runWithLogging(
        TOPIC_NAME,
        topicName.qualifiedName(),
        () -> delegate.updateTopicWithSchema(topicName, patch, modifiedBy));
  }

  @Override
  public void scheduleTouchTopic(TopicName topicName, RequestUser touchedBy) {
    LoggingContext.runWithLogging(
        TOPIC_NAME,
        topicName.qualifiedName(),
        () -> delegate.scheduleTouchTopic(topicName, touchedBy));
  }

  @Override
  public List<String> listQualifiedTopicNames(String groupName) {
    return delegate.listQualifiedTopicNames(groupName);
  }

  @Override
  public List<String> listQualifiedTopicNames() {
    return delegate.listQualifiedTopicNames();
  }

  @Override
  public List<Topic> listTopics(String groupName) {
    return delegate.listTopics(groupName);
  }

  @Override
  public Topic getTopicDetails(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.getTopicDetails(topicName));
  }

  @Override
  public TopicWithSchema getTopicWithSchema(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.getTopicWithSchema(topicName));
  }

  @Override
  public TopicMetrics getTopicMetrics(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.getTopicMetrics(topicName));
  }

  @Override
  public String fetchSingleMessageFromPrimary(
      String brokersClusterName, TopicName topicName, Integer partition, Long offset) {
    return LoggingContext.withLogging(
        TOPIC_NAME,
        topicName.qualifiedName(),
        () ->
            delegate.fetchSingleMessageFromPrimary(
                brokersClusterName, topicName, partition, offset));
  }

  @Override
  public List<String> listTrackedTopicNames() {
    return delegate.listTrackedTopicNames();
  }

  @Override
  public List<String> listTrackedTopicNames(String groupName) {
    return delegate.listTrackedTopicNames(groupName);
  }

  @Override
  public List<String> listFilteredTopicNames(Query<Topic> query) {
    return delegate.listFilteredTopicNames(query);
  }

  @Override
  public List<String> listFilteredTopicNames(String groupName, Query<Topic> query) {
    return delegate.listFilteredTopicNames(groupName, query);
  }

  @Override
  public List<Topic> queryTopic(Query<Topic> query) {
    return delegate.queryTopic(query);
  }

  @Override
  public List<Topic> getAllTopics() {
    return delegate.getAllTopics();
  }

  @Override
  public Optional<byte[]> preview(TopicName topicName, int idx) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.preview(topicName, idx));
  }

  @Override
  public List<MessageTextPreview> previewText(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.previewText(topicName));
  }

  @Override
  public List<TopicNameWithMetrics> queryTopicsMetrics(Query<TopicNameWithMetrics> query) {
    return delegate.queryTopicsMetrics(query);
  }

  @Override
  public TopicStats getStats() {
    return delegate.getStats();
  }

  @Override
  public List<Topic> listForOwnerId(OwnerId ownerId) {
    return delegate.listForOwnerId(ownerId);
  }
}
