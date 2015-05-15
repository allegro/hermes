package pl.allegro.tech.hermes.management.api.validator;

import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

@Component
public class ApiPreconditions {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public <T> void checkConstraints(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
