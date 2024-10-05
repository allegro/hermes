package pl.allegro.tech.hermes.consumers.consumer.receiver;

import java.util.Optional;
import java.util.Set;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;

public interface MessageReceiver {

  /**
   * Retrieves the next available message from the queue.
   *
   * <p>Depending on the context, the returned {@link Optional} can contain:
   *
   * <ul>
   *   <li>A {@link Message} that contains a valid message ready to be sent.
   *   <li>A {@link Message} with the `isFiltered` flag set, indicating that the message has been
   *       filtered and should be skipped during processing or sending.
   *   <li>{@code null}, indicating that there are no messages currently available in the queue.
   * </ul>
   *
   * @return an {@link Optional} containing the next {@link Message} if available; an {@link
   *     Optional} containing a filtered message if it should be skipped; or an empty {@link
   *     Optional} if there are no messages in the queue.
   */
  Optional<Message> next();

  default void stop() {}

  default void update(Subscription newSubscription) {}

  void commit(Set<SubscriptionPartitionOffset> offsets);

  boolean moveOffset(PartitionOffset offset);
}
