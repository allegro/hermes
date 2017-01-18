package pl.allegro.tech.hermes.management.domain.maintainer;

import pl.allegro.tech.hermes.api.Maintainer;
import pl.allegro.tech.hermes.management.infrastructure.crowd.CrowdClient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CrowdMaintainerSource implements HintingMaintainerSource {

    public static final String NAME = "Crowd";

    private final CrowdClient crowdClient;

    public CrowdMaintainerSource(CrowdClient crowdClient) {
        this.crowdClient = crowdClient;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean exists(String maintainerId) {
        return true;
    }

    @Override
    public Maintainer get(String id) {
        return new Maintainer(id, id);
    }

    @Override
    public List<Maintainer> maintainersMatching(String searchString) {
        List<String> stableBase = Arrays.stream(searchString.split(",")).map(String::trim).collect(Collectors.toList());
        String searchedPrefix = stableBase.remove(stableBase.size() - 1);
        return crowdClient.getGroups(searchedPrefix)
                .stream()
                .map(foundName -> Stream.concat(stableBase.stream(), Stream.of(foundName)).collect(Collectors.joining(", ")))
                .map(x -> new Maintainer(x, x))
                .collect(Collectors.toList());
    }

}
