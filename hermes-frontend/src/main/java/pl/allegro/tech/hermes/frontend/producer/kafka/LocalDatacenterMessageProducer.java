package pl.allegro.tech.hermes.frontend.producer.kafka;

import jakarta.inject.Singleton;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;

@Singleton
public class LocalDatacenterMessageProducer implements BrokerMessageProducer {

  private final KafkaMessageSenders kafkaMessageSenders;
  private final MessageToKafkaProducerRecordConverter messageConverter;

  public LocalDatacenterMessageProducer(
      KafkaMessageSenders kafkaMessageSenders,
      MessageToKafkaProducerRecordConverter messageConverter) {
    this.kafkaMessageSenders = kafkaMessageSenders;
    this.messageConverter = messageConverter;
  }

  @Override
  public void send(Message message, CachedTopic cachedTopic, final PublishingCallback callback) {
    ProducerRecord<byte[], byte[]> producerRecord =
        messageConverter.convertToProducerRecord(
            message, cachedTopic.getKafkaTopics().getPrimary().name());

    var producer = kafkaMessageSenders.get(cachedTopic.getTopic());
    Callback wrappedCallback =
        new SendCallback(message, cachedTopic, callback, producer.getDatacenter());
    producer.send(producerRecord, cachedTopic, message, wrappedCallback);
  }

  @Override
  public boolean areAllTopicsAvailable() {
    return kafkaMessageSenders.areAllTopicsAvailable();
  }

  @Override
  public boolean isTopicAvailable(CachedTopic cachedTopic) {
    return kafkaMessageSenders.isTopicAvailable(cachedTopic);
  }

  private static class SendCallback implements org.apache.kafka.clients.producer.Callback {

    private final Message message;
    private final CachedTopic topic;
    private final PublishingCallback callback;
    private final String datacenter;

    public SendCallback(
        Message message, CachedTopic topic, PublishingCallback callback, String datacenter) {
      this.message = message;
      this.topic = topic;
      this.callback = callback;
      this.datacenter = datacenter;
    }

    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
      if (e == null) {
        callback.onEachPublished(message, topic.getTopic(), datacenter);
        callback.onPublished(message, topic.getTopic());
      } else {
        callback.onUnpublished(message, topic.getTopic(), e);
      }
    }
  }
}
