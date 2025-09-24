package pl.allegro.tech.hermes.tracker.consumers.deadletters;

import java.util.List;

import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.Subscription;

import static org.slf4j.LoggerFactory.getLogger;


public class DeadLetters {
  private static final Logger logger = getLogger(DeadLetters.class);
  private final List<DeadRepository> repositories;

  public DeadLetters(List<DeadRepository> repositories) {
    this.repositories = repositories;
  }

  public void send(Subscription subscription, DeadMessage message) {
    for (DeadRepository repository : repositories) {
      if (repository.supports(subscription)) {
        try {
          repository.logDeadLetter(message);
        } catch (Exception e) {
          // Log the error and continue to the next repository
          logger.error(String.format("Failed to send to repository: %s", e.getMessage()), e);
        }
      }
    }
  }

  public void close() {
    repositories.forEach(DeadRepository::close);
  }
}
