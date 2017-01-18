package pl.allegro.tech.hermes.management.domain.maintainer;

import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MaintainerSources implements Iterable<MaintainerSource> {

    private final Map<String, MaintainerSource> maintainerSourcesByNames;

    private final List<MaintainerSource> maintainerSources;

    public MaintainerSources(List<MaintainerSource> maintainerSources) {
        if (maintainerSources.isEmpty()) {
            throw new IllegalArgumentException("At least one maintainer source must be configured");
        }

        this.maintainerSourcesByNames = maintainerSources.stream().collect(
                Collectors.toMap(
                        MaintainerSource::name,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalArgumentException("Duplicate maintainer source " + a.name());
                        }
                )
        );

        this.maintainerSources = ImmutableList.copyOf(maintainerSources);
    }

    public Optional<MaintainerSource> getByName(String name) {
        return Optional.ofNullable(maintainerSourcesByNames.get(name));
    }

    public Iterator<MaintainerSource> iterator() {
        return maintainerSources.iterator();
    }

}
