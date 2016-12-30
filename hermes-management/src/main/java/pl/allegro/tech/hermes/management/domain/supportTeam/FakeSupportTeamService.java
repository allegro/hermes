package pl.allegro.tech.hermes.management.domain.supportTeam;

import pl.allegro.tech.hermes.api.SupportTeam;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FakeSupportTeamService implements SupportTeamService {

    private final List<String> teams = new ArrayList<>();

    public FakeSupportTeamService() {
        teams.add("Scrum Team Alpha");
        teams.add("Scrum Team Beta");
        teams.add("Scrum Team Gamma");
        teams.add("Scrum Team A,Scrum Team B");
        teams.add("Team Red");
        teams.add("Team Blue");
        teams.add("_hadoop");
    }

    @Override
    public List<SupportTeam> getSupportTeams(String searchString) {
        return teams.stream().filter(s -> s.contains(searchString)).map(SupportTeam::new).collect(Collectors.toList());
    }

}
