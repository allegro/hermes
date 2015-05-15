package pl.allegro.tech.hermes.frontend.validator;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

public class Preconditions {

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public Preconditions() {

    }

    public <T> void checkConstraints(T object) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
