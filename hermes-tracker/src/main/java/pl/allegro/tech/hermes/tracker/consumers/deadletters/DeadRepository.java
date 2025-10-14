package pl.allegro.tech.hermes.tracker.consumers.deadletters;

import pl.allegro.tech.hermes.api.Subscription;

public interface DeadRepository {
  void logDeadLetter(DeadMessage message);

  boolean supports(Subscription subscription);

}
