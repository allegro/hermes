package pl.allegro.tech.hermes.management.api.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.Set;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.constraints.AdminPermitted;

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
