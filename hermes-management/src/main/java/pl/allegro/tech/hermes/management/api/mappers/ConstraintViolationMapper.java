package pl.allegro.tech.hermes.management.api.mappers;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.glassfish.jersey.server.validation.ValidationError;
import org.glassfish.jersey.server.validation.internal.ValidationHelper;
import pl.allegro.tech.hermes.api.ErrorCode;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.util.List;

@Provider
public class ConstraintViolationMapper extends AbstractExceptionMapper<ConstraintViolationException> {

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
        List<String> errors = Lists.transform(
                ValidationHelper.constraintViolationToValidationErrors(ex),
                new ValidationErrorConverter()
        );

        return Joiner.on("; ").join(errors);
    }

    private static final class ValidationErrorConverter implements Function<ValidationError, String> {
        @Override
        public String apply(ValidationError input) {
            return input.getPath() + " " + input.getMessage();
        }
    }
}
