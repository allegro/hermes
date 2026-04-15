package pl.allegro.tech.hermes.frontend.producer.kafka;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Routes Hermes topics to specific Kafka producer instances within a pool using round-robin
 * assignment.
 *
 * <p>Assignment is stable: the first time a topic is seen it is assigned the next producer index in
 * round-robin order. Subsequent calls for the same topic always return the same index. This ensures
 * that all messages for a given topic are sent through the same producer, preserving ordering
 * guarantees within a partition.
 *
 * <p>The round-robin assignment guarantees even distribution of topics across pool members,
 * regardless of topic name patterns.
 *
 * <p>With the default pool size of 1, the behavior is identical to the pre-pool architecture (a
 * single producer handles all topics).
 *
 * <p>This class is thread-safe.
 *
 * @see KafkaMessageSenders
 */
class KafkaMessageSenderPoolRouter {

  private final int poolSize;
  private final ConcurrentMap<String, Integer> topicToIndex = new ConcurrentHashMap<>();
  private final AtomicInteger nextIndex = new AtomicInteger(0);

  /**
   * Creates a new pool router with the given pool size.
   *
   * @param poolSize the number of producer instances in the pool, must be at least 1
   * @throws IllegalArgumentException if poolSize is less than 1
   */
  KafkaMessageSenderPoolRouter(int poolSize) {
    if (poolSize < 1) {
      throw new IllegalArgumentException("Pool size must be at least 1, got: " + poolSize);
    }
    this.poolSize = poolSize;
  }

  /**
   * Determines the pool index for the given topic.
   *
   * <p>On the first call for a given topic, the next round-robin index is assigned and stored.
   * Subsequent calls for the same topic return the stored index.
   *
   * @param hermesTopicQualifiedName the fully qualified Hermes topic name (group.topic)
   * @return the pool index in the range [0, poolSize)
   */
  int route(String hermesTopicQualifiedName) {
    return topicToIndex.computeIfAbsent(
        hermesTopicQualifiedName, key -> Math.floorMod(nextIndex.getAndIncrement(), poolSize));
  }

  /**
   * Returns the number of producer instances in the pool.
   *
   * @return the pool size
   */
  int poolSize() {
    return poolSize;
  }

  /**
   * Returns the current distribution of topics across pool members.
   *
   * <p>The returned array has exactly {@code poolSize} elements, where the value at index {@code i}
   * is the number of topics currently assigned to producer {@code i}.
   *
   * @return an array of topic counts per producer index
   */
  int[] getDistribution() {
    int[] counts = new int[poolSize];
    topicToIndex.forEach((topic, index) -> counts[index]++);
    return counts;
  }
}
