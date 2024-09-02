package pl.allegro.tech.hermes.domain.topic;

import java.util.Collection;
import java.util.List;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

public interface TopicRepository {

  boolean topicExists(TopicName topicName);

  void ensureTopicExists(TopicName topicName);

  List<String> listTopicNames(String groupName);

  List<Topic> listTopics(String groupName);

  void createTopic(Topic topic);

  void removeTopic(TopicName topicName);

  void updateTopic(Topic topic);

  void touchTopic(TopicName topicName);

  Topic getTopicDetails(TopicName topicName);

  List<Topic> getTopicsDetails(Collection<TopicName> topicNames);

  List<Topic> listAllTopics();
}
