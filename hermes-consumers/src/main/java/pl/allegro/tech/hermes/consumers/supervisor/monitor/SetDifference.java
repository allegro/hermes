package pl.allegro.tech.hermes.consumers.supervisor.monitor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class SetDifference<E> {

  private final Set<E> added;

  private final Set<E> removed;

  SetDifference(Set<E> added, Set<E> removed) {
    this.added = Collections.unmodifiableSet(new HashSet<>(added));
    this.removed = Collections.unmodifiableSet(new HashSet<>(removed));
  }

  Set<E> added() {
    return added;
  }

  Set<E> removed() {
    return removed;
  }

  boolean containsChanges() {
    return !added.isEmpty() || !removed.isEmpty();
  }

  @Override
  public String toString() {
    return "SetDifference{" + "added=" + added + ", removed=" + removed + '}';
  }
}
