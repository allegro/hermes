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
@Constraint(validatedBy = TimeRangeForTopicRetransmissionValidator.class)
public @interface TimeRangeForTopicRetransmission {
  String message() default
      "Must contain both startTimestamp and endTimestamp for topic retransmission. StartTimestamp must be lower than endTimestamp";

  Class[] groups() default {};

  Class[] payload() default {};
}
