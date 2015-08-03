package pl.allegro.tech.hermes.api.constraints;

import org.junit.Test;
import pl.allegro.tech.hermes.api.EndpointAddress;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class EndpointAddressValidatorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    
    @Test
    public void shouldNotReturnValidationErrorsForValidEndpoint() {
        // when
        Set<ConstraintViolation<EndpointAddress>> violations = validator.validate(EndpointAddress.of("http://some.endpoint.com"));

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    public void shouldValidateEndpointWithInvalidProtocol() {
        // when
        Set<ConstraintViolation<EndpointAddress>> violations = validator.validate(EndpointAddress.of("thisisstupid://some.endpoint.com"));

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    public void shouldValidateEndpointAddressWithInvalidURI() {
        // when
        Set<ConstraintViolation<EndpointAddress>> violations = validator.validate(EndpointAddress.of("jms://{}invalid.endpoint.com"));

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    public void shouldValidateEndpointAddressWithInvalidTemplateURI() {
        // when
        Set<ConstraintViolation<EndpointAddress>> violations = validator.validate(EndpointAddress.of("http://thisistemplate{\\}"));

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    public void shouldValidateEndpointAddressWithValidTemplateURI() {
        // when
        Set<ConstraintViolation<EndpointAddress>> violations = validator.validate(EndpointAddress.of("http://thisistemplate/{name}"));

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    public void shouldValidateUriTemplateWithInvalidHostname() {
        // when
        Set<ConstraintViolation<EndpointAddress>> violations = validator.validate(EndpointAddress.of("http://host_with_invalid_chars/{variable}"));

        // then
        assertThat(violations).hasSize(1);
    }

    @Test
    public void shouldValidateUriWithInvalidHostname() {
        // when
        Set<ConstraintViolation<EndpointAddress>> violations = validator.validate(EndpointAddress.of("http://host_with_invalid_chars.com"));

        // then
        assertThat(violations).hasSize(1);
    }

}