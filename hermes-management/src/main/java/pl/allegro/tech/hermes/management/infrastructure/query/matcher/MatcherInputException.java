package pl.allegro.tech.hermes.management.infrastructure.query.matcher;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class MatcherInputException extends ManagementException {

  public MatcherInputException(String message) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.INVALID_QUERY;
  }
}
