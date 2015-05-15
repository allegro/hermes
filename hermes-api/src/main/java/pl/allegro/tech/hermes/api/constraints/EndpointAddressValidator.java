package pl.allegro.tech.hermes.api.constraints;

import com.damnhandy.uri.template.UriTemplate;
import pl.allegro.tech.hermes.api.EndpointAddress;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class EndpointAddressValidator implements ConstraintValidator<ValidAddress, String> {

    private static final String PROTOCOL_ADDRESS_FORMAT_INVALID = "Endpoint address has invalid format: %s";

    private static final Set<String> AVAILABLE_PROTOCOLS = new HashSet<>();

    static {
        AVAILABLE_PROTOCOLS.add("http");
        AVAILABLE_PROTOCOLS.add("jms");
    }

    public static void addProtocol(String protocol) {
        AVAILABLE_PROTOCOLS.add(protocol);
    }

    @Override
    public void initialize(ValidAddress constraintAnnotation) {

    }

    @Override
    public boolean isValid(String address, ConstraintValidatorContext context) {
        return AVAILABLE_PROTOCOLS.contains(EndpointAddress.extractProtocolFromAddress(address)) && validateWithTemplate(address, context);
    }

    private boolean validateWithTemplate(String address, ConstraintValidatorContext context) {
        try {
            UriTemplate.fromTemplate(address).getVariables();
        } catch (Exception e) {
            createConstraintMessage(context, String.format(PROTOCOL_ADDRESS_FORMAT_INVALID, e.getMessage()));
            return false;
        }

        return true;
    }

    protected void createConstraintMessage(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation()
                .disableDefaultConstraintViolation();
    }
}
