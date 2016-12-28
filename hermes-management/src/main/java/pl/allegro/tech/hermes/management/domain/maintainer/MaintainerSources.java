package pl.allegro.tech.hermes.management.domain.maintainer;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MaintainerSources {

    private final Map<String, MaintainerSource> maintainerSourcesByNames;

    private final List<String> maintainerSourceNames;

    @Autowired
    public MaintainerSources(List<MaintainerSource> maintainerSources) {
        if (maintainerSources.isEmpty()) {
            throw new IllegalArgumentException("At least one maintainer source must be configured");
        }

        this.maintainerSourcesByNames = maintainerSources.stream().collect(
                Collectors.toMap(
                        MaintainerSource::sourceName,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalArgumentException("Duplicate maintainer source " + a.sourceName());
                        }
                )
        );

        this.maintainerSourceNames = ImmutableList.copyOf(maintainerSources.stream().map(MaintainerSource::sourceName).iterator());
    }

    public Optional<MaintainerSource> getByName(String searchedName) {
        return Optional.ofNullable(maintainerSourcesByNames.get(searchedName));
    }

    public List<String> names() {
        return maintainerSourceNames;
    }

}
