package pl.allegro.tech.hermes.management.domain.owner;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class OwnerSourceNotFound extends ManagementException {

  public OwnerSourceNotFound(String name) {
    super("Owner source named '" + name + "' not found");
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.OWNER_SOURCE_NOT_FOUND;
  }
}
