package pl.allegro.tech.hermes.management.domain.supportTeam;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class CouldNotLoadSupportTeamsException extends HermesException {

    public CouldNotLoadSupportTeamsException(Throwable t) {
        super(t);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SUPPORT_TEAMS_COULD_NOT_BE_LOADED;
    }
}
