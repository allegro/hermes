package pl.allegro.tech.hermes.management.domain.owner;

import com.google.common.base.Strings;
import pl.allegro.tech.hermes.api.Owner;
import pl.allegro.tech.hermes.management.infrastructure.crowd.CrowdClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CrowdOwnerSource implements AutocompleteOwnerSource {

    public static final String NAME = "Crowd";

    private final CrowdClient crowdClient;

    public CrowdOwnerSource(CrowdClient crowdClient) {
        this.crowdClient = crowdClient;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean exists(String ownerId) {
        return true;
    }

    @Override
    public Owner get(String id) {
        return new Owner(id, id);
    }

    @Override
    public List<Owner> ownersMatching(String searchString) {
        if (Strings.isNullOrEmpty(searchString)) {
            return Collections.emptyList();
        }

        List<String> stableBase = Arrays.stream(searchString.split(",")).map(String::trim).collect(Collectors.toList());
        String searchedPrefix = stableBase.remove(stableBase.size() - 1);
        return crowdClient.getGroups(searchedPrefix)
                .stream()
                .map(foundName -> Stream.concat(stableBase.stream(), Stream.of(foundName)).collect(Collectors.joining(", ")))
                .map(x -> new Owner(x, x))
                .collect(Collectors.toList());
    }

}
