package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.integrationtests.setup.HermesExtension.brokerOperations;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.common.config.TopicConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.InconsistentKafkaTopic;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class KafkaTopicConfigConsistencyTest {

  @RegisterExtension
  public static final HermesExtension hermes =
      new HermesExtension().withManagementArgs("--management.topic.partitionsPerDc.dc=2");

  @Test
  public void shouldDetectAndReconcileOnlyOwnedConfigsWithoutChangingPartitions() {
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    String kafkaTopicName = brokerOperations.kafkaTopicName(topic);
    int partitions = brokerOperations.getPartitionCount(kafkaTopicName);
    brokerOperations.setTopicConfigs(
        kafkaTopicName,
        Map.of(
            TopicConfig.RETENTION_MS_CONFIG,
            "123",
            TopicConfig.SEGMENT_BYTES_CONFIG,
            "10485760",
            TopicConfig.DELETE_RETENTION_MS_CONFIG,
            "1000"));

    var inconsistencies = hermes.api().getKafkaConfigInconsistencies("primary-dc");
    assertThat(inconsistencies)
        .filteredOn(item -> item.qualifiedTopicName().equals(topic.getQualifiedName()))
        .singleElement()
        .extracting(InconsistentKafkaTopic::kafkaTopicName)
        .isEqualTo(kafkaTopicName);

    assertThat(hermes.api().getKafkaClusters()).contains("primary-dc");
    hermes
        .api()
        .inspectKafkaTopicConfig(topic.getQualifiedName(), "primary-dc")
        .expectStatus()
        .isOk();
    hermes
        .api()
        .syncKafkaTopicConfig(topic.getQualifiedName(), kafkaTopicName, "primary-dc", true)
        .expectStatus()
        .isOk();

    hermes.api().syncKafkaTopicConfigs("primary-dc", true).expectStatus().isOk();
    assertThat(brokerOperations.readTopicConfigs(kafkaTopicName))
        .containsEntry(TopicConfig.RETENTION_MS_CONFIG, "123")
        .containsEntry(TopicConfig.SEGMENT_BYTES_CONFIG, "10485760")
        .containsEntry(TopicConfig.DELETE_RETENTION_MS_CONFIG, "1000");

    hermes.api().syncKafkaTopicConfigs("primary-dc", false).expectStatus().isOk();
    assertThat(brokerOperations.readTopicConfigs(kafkaTopicName))
        .containsEntry(
            TopicConfig.RETENTION_MS_CONFIG,
            String.valueOf(topic.getRetentionTime().getDurationInMillis()))
        .containsEntry(TopicConfig.SEGMENT_BYTES_CONFIG, "10485760")
        .containsEntry(TopicConfig.DELETE_RETENTION_MS_CONFIG, "1000");
    assertThat(brokerOperations.getPartitionCount(kafkaTopicName)).isEqualTo(partitions);
    assertThat(hermes.api().getKafkaConfigInconsistencies("primary-dc"))
        .noneMatch(item -> item.qualifiedTopicName().equals(topic.getQualifiedName()));
  }

  @Test
  public void shouldBootstrapMissingTopicsWithDryRunAndBecomeANoOp() {
    Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());
    String kafkaTopicName = brokerOperations.kafkaTopicName(topic);
    brokerOperations.deleteTopic(kafkaTopicName);
    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> !brokerOperations.topicExists(topic.getQualifiedName()));

    assertThat(hermes.api().bootstrapKafkaCluster("primary-dc", true)).contains(kafkaTopicName);
    assertThat(brokerOperations.topicExists(topic.getQualifiedName())).isFalse();

    assertThat(hermes.api().bootstrapKafkaCluster("primary-dc", false)).contains(kafkaTopicName);
    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> brokerOperations.topicExists(topic.getQualifiedName()));

    assertThat(hermes.api().bootstrapKafkaCluster("primary-dc", false)).isEmpty();
  }
}
