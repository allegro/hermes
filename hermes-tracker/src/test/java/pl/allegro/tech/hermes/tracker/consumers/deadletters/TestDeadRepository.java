package pl.allegro.tech.hermes.tracker.consumers.deadletters;

import java.util.ArrayList;
import java.util.List;
import pl.allegro.tech.hermes.api.Subscription;

public class TestDeadRepository implements DeadRepository {
  public List<DeadMessage> repo = new ArrayList<>();
  private boolean supports;

  public static TestDeadRepository SUPPORTING() {
    return new TestDeadRepository(true);
  }

  public static TestDeadRepository UNSUPPORTING() {
    return new TestDeadRepository(false);
  }

  private TestDeadRepository(boolean supports) {
    this.supports = supports;
  }

  public int count() {
    return repo.size();
  }

  public DeadMessage get(int index) {
    return repo.get(index);
  }

  public boolean isEmpty() {
    return repo.isEmpty();
  }

  @Override
  public void logDeadLetter(DeadMessage message) {
    repo.add(message);
  }

  @Override
  public boolean supports(Subscription subscription) {
    return supports;
  }

  @Override
  public void close() {}
}
