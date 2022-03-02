package pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.cache;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.googlepubsub.GooglePubSubSenderTarget;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class GooglePubSubPublishersCache {

    private static final Logger logger = LoggerFactory.getLogger(GooglePubSubPublishersCache.class);

    private final LoadingCache<GooglePubSubSenderTarget, Publisher> cache;

    public GooglePubSubPublishersCache(LoadingCache<GooglePubSubSenderTarget, Publisher> cache) {
        this.cache = cache;
    }

    public void shutdown() {
        cache.asMap().values().parallelStream().forEach(p -> {
            p.shutdown();
            try {
                p.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Interrupted termination of the PubSub publisher.");
            }
        });
    }

    public Publisher get(GooglePubSubSenderTarget resolvedTarget) throws ExecutionException {
        return cache.get(resolvedTarget);
    }
}