package pl.allegro.tech.hermes.consumers.supervisor.monitor;

import com.google.common.collect.Sets;
import java.util.Set;

class DifferenceCalculator {

  static <E> SetDifference<E> calculate(Set<E> collection1, Set<E> collection2) {
    return new SetDifference<>(
        Sets.difference(collection2, collection1).immutableCopy(),
        Sets.difference(collection1, collection2).immutableCopy());
  }
}
