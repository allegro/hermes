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
@Constraint(validatedBy = TimeRangeAbsentForViewRetransmissionValidator.class)
public @interface TimeRangeAbsentForViewRetransmission {
  String message() default
      "must not contain startTimestamp and endTimestamp when source view is given";

  Class[] groups() default {};

  Class[] payload() default {};
}
