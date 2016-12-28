package pl.allegro.tech.hermes.management.domain.maintainer;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class MaintainerSourceNotFound extends ManagementException {

    public MaintainerSourceNotFound(String name) {
        super("Maintainer source named '" + name + "' not found");
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.MAINTAINER_SOURCE_NOT_FOUND;
    }

}
