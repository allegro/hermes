package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;

public interface MessageContentReader {
  UnwrappedMessageContent read(ConsumerRecord<byte[], byte[]> message, ContentType contentType);
}
