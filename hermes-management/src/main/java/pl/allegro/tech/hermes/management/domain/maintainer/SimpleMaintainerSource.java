package pl.allegro.tech.hermes.management.domain.maintainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SupportTeam;
import pl.allegro.tech.hermes.management.domain.supportTeam.SupportTeamService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Order(0)
public class SimpleMaintainerSource implements MaintainerSource {

    private final SupportTeamService supportTeamService;

    @Autowired
    public SimpleMaintainerSource(SupportTeamService supportTeamService) {
        this.supportTeamService = supportTeamService;
    }

    @Override
    public String sourceName() {
        return "simple";
    }

    @Override
    public boolean exists(String maintainerId) {
        return true;
    }

    @Override
    public String nameForId(String maintainerId) {
        return maintainerId;
    }

    @Override
    public List<String> maintainersMatching(String searchString) {
        return supportTeamService.getSupportTeams(searchString)
                .stream()
                .map(SupportTeam::getName)
                .collect(Collectors.toList());
    }

}
