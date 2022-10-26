package pl.allegro.tech.hermes.management.api.validator;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.constraints.AdminPermitted;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;

@Component
public class ApiPreconditions {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public <T> void checkConstraints(T object, boolean skipAdminPermitted) {
        Class<?>[] groups = groupsToValidate(skipAdminPermitted);
        Set<ConstraintViolation<T>> violations = validator.validate(object, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private Class<?>[] groupsToValidate(boolean skipAdminPermitted) {
        if (skipAdminPermitted) {
            return new Class[] {Default.class};
        } else {
            return new Class[] {Default.class, AdminPermitted.class};
        }
    }
}
