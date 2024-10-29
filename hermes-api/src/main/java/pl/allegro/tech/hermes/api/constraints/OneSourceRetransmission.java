package pl.allegro.tech.hermes.api.constraints;

import static java.lang.annotation.ElementType.TYPE;

import jakarta.validation.Constraint;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
@Constraint(validatedBy = OneSourceRetransmissionValidator.class)
public @interface OneSourceRetransmission {
  String message() default
      "must contain one defined source of retransmission data - source topic or source view";

  Class[] groups() default {};

  Class[] payload() default {};
}
