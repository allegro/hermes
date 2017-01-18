package pl.allegro.tech.hermes.management.infrastructure.crowd;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

import java.util.List;

public interface CrowdClient {

    List<String> getGroups(String searchString) throws CouldNotLoadCrowdGroupsException;

    class CouldNotLoadCrowdGroupsException extends HermesException {

        CouldNotLoadCrowdGroupsException(Throwable t) {
            super(t);
        }

        @Override
        public ErrorCode getCode() {
            return ErrorCode.CROWD_GROUPS_COULD_NOT_BE_LOADED;
        }
    }
}
