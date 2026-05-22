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
  private final Map<T, Instant> lastResetInstant = new HashMap<>();

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
    lastResetInstant.clear();
  }

  /*
   * Resets the client for the given target if it has not been reset in the last 30 seconds.
   */
  public synchronized void reset(T resolvedTarget) {
    Instant lastResetInstantForTarget = lastResetInstant.get(resolvedTarget);
    Instant currentInstant = Instant.now();
    if (lastResetInstantForTarget == null
        || lastResetInstantForTarget.plusSeconds(30).isBefore(currentInstant)) {
      C existingClient = clients.get(resolvedTarget);
      if (existingClient != null) {
        try {
          C newClient = createClient(resolvedTarget);
          if (newClient != null) {
            clients.put(resolvedTarget, newClient);
            existingClient.shutdown();
          } else {
            logger.error(
                "Created client is null for target {} during reset, keeping existing client",
                resolvedTarget);
          }
        } catch (IOException e) {
          logger.error("Failed to create client for target {} after reset", resolvedTarget, e);
        }
        lastResetInstant.put(resolvedTarget, currentInstant);
      }
    }
  }

  protected abstract C createClient(T resolvedTarget) throws IOException;
}
