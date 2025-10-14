package pl.allegro.tech.hermes.tracker.consumers.deadletters;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;
import org.slf4j.Logger;
import pl.allegro.tech.hermes.api.Subscription;

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
          logger.error("Failed to send message to dead letter repository.\nsubscription name: {}\ndeadletter repository: {}\nmessage id: {}", subscription.getName(), repository, message.getMessageId(), e);
        }
      }
    }
  }

  public void close() {

  }
}
