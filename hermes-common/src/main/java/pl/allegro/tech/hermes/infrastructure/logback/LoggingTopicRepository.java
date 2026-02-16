package pl.allegro.tech.hermes.infrastructure.logback;

import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import java.util.Collection;
import java.util.List;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.logging.LoggingContext;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;

public class LoggingTopicRepository implements TopicRepository {

  private final TopicRepository delegate;

  public LoggingTopicRepository(TopicRepository delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean topicExists(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.topicExists(topicName));
  }

  @Override
  public void ensureTopicExists(TopicName topicName) {
    LoggingContext.runWithLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.ensureTopicExists(topicName));
  }

  @Override
  public void createTopic(Topic topic) {
    LoggingContext.runWithLogging(
        TOPIC_NAME, topic.getQualifiedName(), () -> delegate.createTopic(topic));
  }

  @Override
  public void removeTopic(TopicName topicName) {
    LoggingContext.runWithLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.removeTopic(topicName));
  }

  @Override
  public void updateTopic(Topic topic) {
    LoggingContext.runWithLogging(
        TOPIC_NAME, topic.getQualifiedName(), () -> delegate.updateTopic(topic));
  }

  @Override
  public void touchTopic(TopicName topicName) {
    LoggingContext.runWithLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.touchTopic(topicName));
  }

  @Override
  public Topic getTopicDetails(TopicName topicName) {
    return LoggingContext.withLogging(
        TOPIC_NAME, topicName.qualifiedName(), () -> delegate.getTopicDetails(topicName));
  }

  @Override
  public List<String> listTopicNames(String groupName) {
    return delegate.listTopicNames(groupName);
  }

  @Override
  public List<Topic> listTopics(String groupName) {
    return delegate.listTopics(groupName);
  }

  @Override
  public List<Topic> getTopicsDetails(Collection<TopicName> topicNames) {
    return delegate.getTopicsDetails(topicNames);
  }

  @Override
  public List<Topic> listAllTopics() {
    return delegate.listAllTopics();
  }
}
