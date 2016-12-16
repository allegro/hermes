package pl.allegro.tech.hermes.management.domain.maintainer;

import avro.shaded.com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MaintainerSources {

    private final Map<String, MaintainerSource> maintainerSourcesByNames;

    private final List<String> maintainerSourceNames;

    @Autowired
    public MaintainerSources(List<MaintainerSource> maintainerSources) {
        this.maintainerSourcesByNames = maintainerSources.stream().collect(
                Collectors.toMap(
                        MaintainerSource::sourceName,
                        Function.identity(),
                        (a, b) -> { throw new IllegalStateException("Duplicate maintainer source " + a.sourceName()); }
                )
        );

        this.maintainerSourceNames = ImmutableList.copyOf(maintainerSources.stream().map(MaintainerSource::sourceName).iterator());
    }

    public MaintainerSource getByName(String searchedName) {
        return Optional.ofNullable(maintainerSourcesByNames.get(searchedName)).orElseThrow(() -> new MaintainerSourceNotFound(searchedName));
    }

    public List<String> names() {
        return maintainerSourceNames;
    }

    public class MaintainerSourceNotFound extends RuntimeException {

        MaintainerSourceNotFound(String name) {
            super("Maintainer source named '" + name + "' not found");
        }

    }
}
