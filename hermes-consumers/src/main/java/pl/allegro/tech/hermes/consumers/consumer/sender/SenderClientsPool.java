package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SenderClientsPool<T extends SenderTarget, C extends SenderClient> {

  private static final Logger logger = LoggerFactory.getLogger(SenderClientsPool.class);

  private final Map<T, C> clients = new HashMap<>();
  private final Map<T, Integer> counters = new HashMap<>();
  private final Map<T, Instant> lastReleaseAllDate = new HashMap<>();

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
    lastReleaseAllDate.clear();
  }

  /*
   * Resets the client for the given target if it has not been reset in the last 30 seconds.
   */
  public synchronized void reset(T resolvedTarget) {
    Instant lastReleaseDate = lastReleaseAllDate.get(resolvedTarget);
    Instant currentInstant = Instant.now();
    if (lastReleaseDate == null || lastReleaseDate.plusSeconds(30).isBefore(currentInstant)) {
      C existingClient = clients.get(resolvedTarget);
      if (existingClient != null) {
        clients.remove(resolvedTarget).shutdown();
        try {
          C client = createClient(resolvedTarget);
          if (client != null) {
            clients.put(resolvedTarget, client);
          }
        } catch (IOException e) {
          logger.error("Failed to create client for target {} after reset", resolvedTarget, e);
        }
        lastReleaseAllDate.put(resolvedTarget, currentInstant);
      }
    }
  }

  protected abstract C createClient(T resolvedTarget) throws IOException;
}
