package pl.allegro.tech.hermes.frontend.producer.kafka;

import java.util.List;

/**
 * A pool of {@link KafkaMessageSender} instances for a single ack configuration in a single
 * datacenter. Topics are deterministically routed to a specific sender within the pool using {@link
 * KafkaMessageSenderPoolRouter}.
 *
 * <p>This distributes topic-partitions across multiple Kafka producers, reducing the per-producer
 * partition count and mitigating the O(n) overhead in the Kafka 3.x Sender thread's {@code
 * RecordAccumulator.ready()} call.
 *
 * <p>With a pool size of 1, this behaves identically to a single producer — no routing overhead, no
 * behavioral difference.
 *
 * @see KafkaMessageSenderPoolRouter
 * @see KafkaMessageSenders
 */
class KafkaMessageSenderPool {

  private final List<KafkaMessageSender<byte[], byte[]>> senders;
  private final KafkaMessageSenderPoolRouter router;

  KafkaMessageSenderPool(List<KafkaMessageSender<byte[], byte[]>> senders) {
    if (senders == null || senders.isEmpty()) {
      throw new IllegalArgumentException("Senders list must not be null or empty");
    }
    this.senders = senders;
    this.router = new KafkaMessageSenderPoolRouter(senders.size());
  }

  /**
   * Returns the sender assigned to the given topic based on pool routing.
   *
   * @param hermesTopicQualifiedName the qualified Hermes topic name used for routing
   * @return the assigned {@link KafkaMessageSender} from the pool
   */
  KafkaMessageSender<byte[], byte[]> get(String hermesTopicQualifiedName) {
    return senders.get(router.route(hermesTopicQualifiedName));
  }

  /**
   * Returns all senders in the pool. Used for metrics aggregation, closing, and metadata loading.
   */
  List<KafkaMessageSender<byte[], byte[]>> allSenders() {
    return senders;
  }

  /**
   * Returns the datacenter this pool belongs to (all senders in a pool share the same datacenter).
   */
  String getDatacenter() {
    return senders.get(0).getDatacenter();
  }

  /** Closes all senders in the pool. */
  void close() {
    senders.forEach(KafkaMessageSender::close);
  }
}
