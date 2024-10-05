package pl.allegro.tech.hermes.frontend.validator;

import com.google.common.base.Joiner;
import java.util.List;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class InvalidMessageException extends HermesException {

  public InvalidMessageException(String msg, List<String> reasons) {
    super(String.format("%s Errors: %s", msg, Joiner.on(";").join(reasons)));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.FORMAT_ERROR;
  }
}
