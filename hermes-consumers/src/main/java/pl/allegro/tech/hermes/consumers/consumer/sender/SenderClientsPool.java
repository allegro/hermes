package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pool of sender clients. It creates and keeps clients for different targets.
 * It is thread-safe.
 *
 * @param <T> type of the target
 * @param <C> type of the client
 */
public abstract class SenderClientsPool<T extends SenderTarget, C extends SenderClient<T>> {

  private static final Logger logger = LoggerFactory.getLogger(SenderClientsPool.class);

  private final Map<T, C> clients = new ConcurrentHashMap<T, C>();

  public C acquire(T resolvedTarget) throws IOException {
      return clients.computeIfAbsent(resolvedTarget, key -> {
          try {
              return createClient(key);
          } catch (IOException e) {
              throw new RuntimeException(e);
          }
      });
  }


  public void release(T resolvedTarget) {
    C client = clients.remove(resolvedTarget);
    if (client != null) {
        client.shutdown();
    } else {
        logger.warn("Attempt to release client that is not acquired");
    }
  }

  public synchronized void shutdown() {
    clients.values().forEach(SenderClient::shutdown);
    clients.clear();
  }

  protected abstract C createClient(T resolvedTarget) throws IOException;
}
