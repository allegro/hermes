package pl.allegro.tech.hermes.management.assertions;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.api.SearchItem;
import pl.allegro.tech.hermes.api.SearchItemType;

public abstract class SearchItemAssertion<
        Self extends SearchItemAssertion<Self, Actual>, Actual extends SearchItem>
    extends AbstractAssert<Self, Actual> {
  protected SearchItemAssertion(Actual actual, Class<?> selfType) {
    super(actual, selfType);
  }

  @SuppressWarnings("unchecked")
  public Self hasName(String expectedName) {
    Assertions.assertThat(actual.name()).isEqualTo(expectedName);
    return (Self) this;
  }

  @SuppressWarnings("unchecked")
  public Self hasType(SearchItemType expectedType) {
    Assertions.assertThat(actual.type()).isEqualTo(expectedType);
    return (Self) this;
  }
}
