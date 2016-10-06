package pl.allegro.tech.hermes.management.domain.supportTeam;

import pl.allegro.tech.hermes.api.SupportTeam;

import java.util.List;

public interface SupportTeamService {

    List<SupportTeam> getSupportTeams(String searchString);
}
