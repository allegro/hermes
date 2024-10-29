package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.management.config.kafka.KafkaProperties;

public class KafkaConsumerManager {

  private final KafkaNamesMapper kafkaNamesMapper;
  private final String brokerList;
  private final KafkaProperties kafkaProperties;

  public KafkaConsumerManager(
      KafkaProperties kafkaProperties, KafkaNamesMapper kafkaNamesMapper, String brokerList) {
    this.kafkaNamesMapper = kafkaNamesMapper;
    this.brokerList = brokerList;
    this.kafkaProperties = kafkaProperties;
  }

  public KafkaConsumer<byte[], byte[]> createConsumer(SubscriptionName subscription) {
    ConsumerGroupId groupId = kafkaNamesMapper.toConsumerGroupId(subscription);
    return new KafkaConsumer<>(properties(groupId));
  }

  private Properties properties(ConsumerGroupId groupId) {
    Properties props = new Properties();
    props.put(BOOTSTRAP_SERVERS_CONFIG, brokerList);
    props.put(GROUP_ID_CONFIG, groupId.asString());
    props.put(ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(REQUEST_TIMEOUT_MS_CONFIG, 5000);
    props.put(DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);
    props.put(
        KEY_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    props.put(
        VALUE_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.ByteArrayDeserializer");
    if (kafkaProperties.getAuthentication().isEnabled()) {
      props.put(SASL_MECHANISM, kafkaProperties.getAuthentication().getMechanism());
      props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getAuthentication().getProtocol());
      props.put(SASL_JAAS_CONFIG, kafkaProperties.getAuthentication().getJaasConfig());
    }
    return props;
  }
}
