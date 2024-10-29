package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CacheListeners {

  private static final Logger logger = LoggerFactory.getLogger(CacheListeners.class);

  private final Queue<Consumer<PathChildrenCacheEvent>> callbacks = new ConcurrentLinkedQueue<>();

  void addListener(Consumer<PathChildrenCacheEvent> callback) {
    callbacks.add(callback);
  }

  void call(PathChildrenCacheEvent event) {
    for (Consumer<PathChildrenCacheEvent> callback : callbacks) {
      try {
        callback.accept(event);
      } catch (Exception exception) {
        logger.error(
            "Failed to run callback action {} for event with data: {}",
            callback.getClass().getSimpleName(),
            event.getData(),
            exception);
      }
    }
  }
}
