package pl.allegro.tech.hermes.benchmark.consumer;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffsets;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.receiver.MessageReceiver;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

public class InMemoryMessageReceiver implements MessageReceiver {
  private static final int PARTITION_ID = 1;
  private final ArrayBlockingQueue<Message> messages;

  public InMemoryMessageReceiver(int messagesCount) {
    CompiledSchema<Schema> compiledSchema = new AvroUser("Bob", 50, "blue").getCompiledSchema();
    messages = new ArrayBlockingQueue<>(messagesCount);
    for (int i = 0; i < messagesCount; i++) {
      PartitionOffset partitionOffset =
          new PartitionOffset(KafkaTopicName.valueOf("test"), i, PARTITION_ID);
      AvroUser user = new AvroUser("Bob", i, "blue");
      Message message =
          Message.message()
              .withData(user.asBytes())
              .withSchema(compiledSchema)
              .withPartitionOffset(partitionOffset)
              .withContentType(ContentType.AVRO)
              .build();
      try {
        messages.put(message);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public Optional<Message> next() {
    Message poll = messages.poll();
    if (poll == null) {
      return Optional.empty();
    } else {
      return Optional.of(poll);
    }
  }

  @Override
  public void commit(Set<SubscriptionPartitionOffset> offsets) {}

  @Override
  public PartitionOffsets moveOffset(PartitionOffsets offsets) {
    return offsets;
  }

  @Override
  public Set<Integer> getAssignedPartitions() {
    return Set.of(PARTITION_ID);
  }
}
