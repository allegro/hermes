package pl.allegro.tech.hermes.management.api.mappers;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import org.glassfish.jersey.server.validation.ValidationErrorData;
import org.glassfish.jersey.server.validation.internal.ValidationHelper;
import pl.allegro.tech.hermes.api.ErrorCode;

@Provider
public class ConstraintViolationMapper
    extends AbstractExceptionMapper<ConstraintViolationException> {

  @Override
  Response.Status httpStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  ErrorCode errorCode() {
    return ErrorCode.VALIDATION_ERROR;
  }

  @Override
  public String errorMessage(ConstraintViolationException exception) {
    return prepareMessage(exception);
  }

  private String prepareMessage(ConstraintViolationException ex) {
    List<String> errors =
        Lists.transform(
            ValidationHelper.constraintViolationToValidationErrors(ex),
            new ValidationErrorDataConverter());

    return Joiner.on("; ").join(errors);
  }

  private static final class ValidationErrorDataConverter
      implements Function<ValidationErrorData, String> {
    @Override
    public String apply(ValidationErrorData input) {
      return input.getPath() + " " + input.getMessage();
    }
  }
}
