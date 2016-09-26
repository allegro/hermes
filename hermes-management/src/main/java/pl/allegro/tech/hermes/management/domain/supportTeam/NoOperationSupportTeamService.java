package pl.allegro.tech.hermes.management.domain.supportTeam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.SupportTeam;

import java.util.Collections;
import java.util.List;

public class NoOperationSupportTeamService implements SupportTeamService {

    private static final String NO_OP_MESSAGE = "No SupportTeamService implementation set, using default no-operation implementation";
    private static final Logger logger = LoggerFactory.getLogger(NoOperationSupportTeamService.class);


    @Override
    public List<SupportTeam> getSupportTeams(String searchString) {
        logger.info(NO_OP_MESSAGE);
        return Collections.emptyList();
    }
}
