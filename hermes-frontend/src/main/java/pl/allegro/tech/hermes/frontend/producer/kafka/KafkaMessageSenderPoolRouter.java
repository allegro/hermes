package pl.allegro.tech.hermes.frontend.producer.kafka;

/**
 * Routes Hermes topics to specific Kafka producer instances within a pool.
 *
 * <p>Assignment is stable: the same topic always routes to the same producer index for a given pool
 * size. This ensures that all messages for a given topic are sent through the same producer,
 * preserving ordering guarantees within a partition.
 *
 * <p>The routing uses {@code Math.floorMod(topicName.hashCode(), poolSize)} which guarantees a
 * non-negative result even for negative hash codes, providing uniform distribution across pool
 * indices [0, poolSize).
 *
 * <p>With the default pool size of 1, the behavior is identical to the pre-pool architecture (a
 * single producer handles all topics).
 *
 * @see KafkaMessageSenders
 */
record KafkaMessageSenderPoolRouter(int poolSize) {

  /**
   * Creates a new pool router with the given pool size.
   *
   * @param poolSize the number of producer instances in the pool, must be at least 1
   * @throws IllegalArgumentException if poolSize is less than 1
   */
  KafkaMessageSenderPoolRouter {
    if (poolSize < 1) {
      throw new IllegalArgumentException("Pool size must be at least 1, got: " + poolSize);
    }
  }

  /**
   * Determines the pool index for the given topic.
   *
   * <p>The routing is based on the hash code of the Hermes qualified topic name (e.g. {@code
   * "group.topic"}). The same topic name will always produce the same index for a given pool size.
   *
   * @param hermesTopicQualifiedName the fully qualified Hermes topic name (group.topic)
   * @return the pool index in the range [0, poolSize)
   */
  int route(String hermesTopicQualifiedName) {
    return Math.floorMod(hermesTopicQualifiedName.hashCode(), poolSize);
  }

  /**
   * Returns the number of producer instances in the pool.
   *
   * @return the pool size
   */
  @Override
  public int poolSize() {
    return poolSize;
  }
}
