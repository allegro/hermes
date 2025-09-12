package pl.allegro.tech.hermes.tracker.consumers.deadletters;

import java.util.List;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.tracker.consumers.*;

public class DeadLetters {
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
          System.err.println("Failed to send to repository: " + e.getMessage());
        }
      }
    }
  }

  public void close() {
    repositories.forEach(DeadRepository::close);
  }
}
