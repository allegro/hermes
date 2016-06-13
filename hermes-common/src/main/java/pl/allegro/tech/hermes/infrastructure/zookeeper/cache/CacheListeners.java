package pl.allegro.tech.hermes.infrastructure.zookeeper.cache;

import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

class CacheListeners {

    private static final Logger logger = LoggerFactory.getLogger(CacheListeners.class);

    // NOTE: we probably don't need to be bothered by this synchronization, as adding occurs only on startup
    // and this is called relatively rare
    private final List<Consumer<PathChildrenCacheEvent>> callbacks = Collections.synchronizedList(new ArrayList<>());

    void addListener(Consumer<PathChildrenCacheEvent> callback) {
        callbacks.add(callback);
    }

    void call(PathChildrenCacheEvent event) {
        for (Consumer<PathChildrenCacheEvent> callback : callbacks) {
            try {
                callback.accept(event);
            } catch (Exception exception) {
                logger.error("Failed to run callback action {}", callback.getClass().getSimpleName(), exception);
            }
        }
    }
}
