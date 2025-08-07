package pl.allegro.tech.hermes.consumers.consumer.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class SenderClientsPool<T extends SenderTarget, C extends SenderClient> {

    private static final Logger logger = LoggerFactory.getLogger(SenderClientsPool.class);

    private final Map<T, C> clients = new HashMap<>();
    private final Map<T, Integer> counters = new HashMap<>();

     public synchronized C acquire(T resolvedTarget) throws IOException {
        C client = clients.get(resolvedTarget);
        if (client == null) {
            client = createClient(resolvedTarget);
        }
        clients.put(resolvedTarget, client);
        Integer counter = counters.getOrDefault(resolvedTarget, 0);
        counters.put(resolvedTarget, ++counter);
        return client;
    }

    public synchronized void release(T resolvedTarget) {
        Integer counter = counters.getOrDefault(resolvedTarget, 0);
        if (counter == 0) {
            logger.warn("Attempt to release client that is not acquired");
        } else if (counter == 1) {
            counters.remove(resolvedTarget);
            C client = clients.remove(resolvedTarget);
            client.shutdown();
        } else if (counter > 1) {
            counters.put(resolvedTarget, --counter);
        }
    }

    public synchronized void shutdown() {
        clients.values().forEach(SenderClient::shutdown);
        clients.clear();
        counters.clear();
    }

    protected abstract C createClient(T resolvedTarget) throws IOException;
}
