package pl.allegro.tech.hermes.management.domain.consistency;

import static java.util.Arrays.asList;
import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.config.kafka.KafkaClustersProperties;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.topic.TopicManagement;
import pl.allegro.tech.hermes.management.infrastructure.kafka.MultiDCAwareService;

@Component
public class KafkaHermesConsistencyService {

  private static final Logger logger = LoggerFactory.getLogger(KafkaHermesConsistencyService.class);

  private static final String AVRO_SUFFIX = "_avro";
  private static final List<String> IGNORED_TOPIC = asList("__consumer_offsets");
  private final TopicManagement topicManagement;
  private final MultiDCAwareService multiDCAwareService;
  private final KafkaClustersProperties kafkaClustersProperties;

  public KafkaHermesConsistencyService(
      TopicManagement topicManagement,
      MultiDCAwareService multiDCAwareService,
      KafkaClustersProperties kafkaClustersProperties) {
    this.topicManagement = topicManagement;
    this.kafkaClustersProperties = kafkaClustersProperties;
    this.multiDCAwareService = multiDCAwareService;
  }

  public Set<String> listInconsistentTopics() {
    List<String> topicsFromHermes = topicManagement.listQualifiedTopicNames();

    return multiDCAwareService.listTopicFromAllDC().stream()
        .filter(
            topic ->
                !IGNORED_TOPIC.contains(topic)
                    && !topicsFromHermes.contains(mapToHermesFormat(topic)))
        .collect(Collectors.toSet());
  }

  public void removeTopic(String topicName, RequestUser requestUser) {
    logger
        .atInfo()
        .addKeyValue(TOPIC_NAME, topicName)
        .log(
            "Removing topic {} only on brokers. Requested by {}",
            topicName,
            requestUser.getUsername());
    multiDCAwareService.removeTopicByName(topicName);
    logger
        .atInfo()
        .addKeyValue(TOPIC_NAME, topicName)
        .log(
            "Successfully removed topic {} on brokers. Requested by {}",
            topicName,
            requestUser.getUsername());
  }

  private String mapToHermesFormat(String topic) {
    String prefix =
        kafkaClustersProperties.getDefaultNamespace()
            + kafkaClustersProperties.getNamespaceSeparator();

    int beginIndex = topic.startsWith(prefix) ? prefix.length() : 0;
    int endIndex =
        topic.endsWith(AVRO_SUFFIX) ? topic.length() - AVRO_SUFFIX.length() : topic.length();

    return topic.substring(beginIndex, endIndex);
  }
}
