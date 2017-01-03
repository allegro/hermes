package pl.allegro.tech.hermes.management.domain.maintainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Maintainer;
import pl.allegro.tech.hermes.management.domain.supportTeam.SupportTeamService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(0)
public class SimpleMaintainerSource implements MaintainerSource {

    public static final String NAME = "Simple";

    private final SupportTeamService supportTeamService;

    @Autowired
    public SimpleMaintainerSource(SupportTeamService supportTeamService) {
        this.supportTeamService = supportTeamService;
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
        return supportTeamService.getSupportTeams(searchString)
                .stream()
                .map(st -> new Maintainer(st.getName(), st.getName()))
                .collect(Collectors.toList());
    }

}
