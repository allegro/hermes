package pl.allegro.tech.hermes.management.domain.owner;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class OwnerSources implements Iterable<OwnerSource> {

  private final Map<String, OwnerSource> ownerSourcesByNames;

  private final List<OwnerSource> ownerSources;

  public OwnerSources(List<OwnerSource> ownerSources) {
    if (ownerSources.isEmpty()) {
      throw new IllegalArgumentException("At least one owner source must be configured");
    }

    this.ownerSourcesByNames =
        ownerSources.stream()
            .collect(
                Collectors.toMap(
                    OwnerSource::name,
                    Function.identity(),
                    (a, b) -> {
                      throw new IllegalArgumentException("Duplicate owner source " + a.name());
                    }));

    this.ownerSources = ImmutableList.copyOf(ownerSources);
  }

  public Optional<OwnerSource> getByName(String name) {
    return Optional.ofNullable(ownerSourcesByNames.get(name));
  }

  public OwnerSource.Autocompletion getAutocompletionFor(String name) {
    OwnerSource source = getByName(name).orElseThrow(() -> new OwnerSourceNotFound(name));
    return source.autocompletion().orElseThrow(() -> new AutocompleteNotSupportedException(source));
  }

  public Iterator<OwnerSource> iterator() {
    return ownerSources.iterator();
  }

  private static class AutocompleteNotSupportedException extends HermesException {

    AutocompleteNotSupportedException(OwnerSource source) {
      super("Owner source '" + source.name() + "' doesn't support autocomplete");
    }

    @Override
    public ErrorCode getCode() {
      return ErrorCode.OWNER_SOURCE_DOESNT_SUPPORT_AUTOCOMPLETE;
    }
  }
}
