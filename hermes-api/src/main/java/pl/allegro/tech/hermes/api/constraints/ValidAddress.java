package pl.allegro.tech.hermes.api.constraints;

import javax.validation.Constraint;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE, METHOD, FIELD, CONSTRUCTOR, PARAMETER, ANNOTATION_TYPE })
@Constraint(validatedBy = EndpointAddressValidator.class)
public @interface ValidAddress {
    String message();

    Class[] groups() default { };

    Class[] payload() default { };
}
