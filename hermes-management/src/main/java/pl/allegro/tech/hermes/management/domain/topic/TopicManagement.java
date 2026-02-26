package pl.allegro.tech.hermes.management.domain.topic;

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
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;

public interface TopicManagement {

  void createTopicWithSchema(
      TopicWithSchema topicWithSchema, RequestUser createdBy, CreatorRights isAllowedToManage);

  void removeTopicWithSchema(Topic topic, RequestUser removedBy);

  void updateTopicWithSchema(TopicName topicName, PatchData patch, RequestUser modifiedBy);

  void scheduleTouchTopic(TopicName topicName, RequestUser touchedBy);

  List<String> listQualifiedTopicNames(String groupName);

  List<String> listQualifiedTopicNames();

  List<Topic> listTopics(String groupName);

  Topic getTopicDetails(TopicName topicName);

  TopicWithSchema getTopicWithSchema(TopicName topicName);

  TopicMetrics getTopicMetrics(TopicName topicName);

  String fetchSingleMessageFromPrimary(
      String brokersClusterName, TopicName topicName, Integer partition, Long offset);

  List<String> listTrackedTopicNames();

  List<String> listTrackedTopicNames(String groupName);

  List<String> listFilteredTopicNames(Query<Topic> query);

  List<String> listFilteredTopicNames(String groupName, Query<Topic> query);

  List<Topic> queryTopic(Query<Topic> query);

  List<Topic> getAllTopics();

  Optional<byte[]> preview(TopicName topicName, int idx);

  List<MessageTextPreview> previewText(TopicName topicName);

  List<TopicNameWithMetrics> queryTopicsMetrics(Query<TopicNameWithMetrics> query);

  TopicStats getStats();

  List<Topic> listForOwnerId(OwnerId ownerId);
}
