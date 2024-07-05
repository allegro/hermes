package pl.allegro.tech.hermes.api.constraints;

import jakarta.validation.Constraint;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
@Constraint(validatedBy = OneSourceRetransmissionValidator.class)
public @interface OneSourceRetransmission {
    String message() default "Only one source of retransmission data is allowed - source topic or source view";

    Class[] groups() default {};

    Class[] payload() default {};
}
