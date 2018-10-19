package pl.allegro.tech.hermes.api.constraints;

import javax.validation.ConstraintValidatorContext;

abstract class BaseValidator {

    void createConstraintMessage(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation()
                .disableDefaultConstraintViolation();
    }
}
