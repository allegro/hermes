package pl.allegro.tech.hermes.management.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.api.SearchItem;

public abstract class SearchItemAssertion<
        Self extends SearchItemAssertion<Self, Actual>, Actual extends SearchItem>
    extends AbstractAssert<Self, Actual> {
  protected SearchItemAssertion(Actual actual, Class<?> selfType) {
    super(actual, selfType);
  }

  public Self hasName(String expectedName) {
    Assertions.assertThat(actual.name()).isEqualTo(expectedName);
    return (Self) this;
  }

  public Self hasType(String expectedType) {
    Assertions.assertThat(actual.type()).isEqualTo(expectedType);
    return (Self) this;
  }
}
