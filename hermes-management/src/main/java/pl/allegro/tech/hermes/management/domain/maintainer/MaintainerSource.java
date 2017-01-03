package pl.allegro.tech.hermes.management.domain.maintainer;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Maintainer;
import pl.allegro.tech.hermes.management.domain.ManagementException;

import java.util.List;

public interface MaintainerSource {

    String name();

    boolean exists(String maintainerId);

    Maintainer get(String id) throws MaintainerNotFound;

    List<Maintainer> maintainersMatching(String searchString);

    class MaintainerNotFound extends ManagementException {

        public MaintainerNotFound(String source, String id) {
            super("Maintainer of id '" + id + "' not found in source " + source);
        }

        @Override
        public ErrorCode getCode() {
            return ErrorCode.MAINTAINER_NOT_FOUND;
        }
    }

}
