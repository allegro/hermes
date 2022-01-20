package pl.allegro.tech.hermes.api.constraints;

import com.damnhandy.uri.template.UriTemplate;
import pl.allegro.tech.hermes.api.EndpointAddress;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class EndpointAddressValidator implements ConstraintValidator<ValidAddress, String> {

    private static final String PROTOCOL_ADDRESS_FORMAT_INVALID = "Endpoint address has invalid format: %s";

    private static final Set<String> AVAILABLE_PROTOCOLS = new HashSet<>();

    static {
        AVAILABLE_PROTOCOLS.add("http");
        AVAILABLE_PROTOCOLS.add("https");
        AVAILABLE_PROTOCOLS.add("jms");
        AVAILABLE_PROTOCOLS.add("pubsub");
    }

    public static void addProtocol(String protocol) {
        AVAILABLE_PROTOCOLS.add(protocol);
    }

    @Override
    public void initialize(ValidAddress constraintAnnotation) {

    }

    @Override
    public boolean isValid(String address, ConstraintValidatorContext context) {
        return isValidProtocol(address, context) && isValidUriTemplate(address, context);
    }

    private boolean isValidProtocol(String address, ConstraintValidatorContext context) {
        try {
            return AVAILABLE_PROTOCOLS.contains(EndpointAddress.extractProtocolFromAddress(address));
        } catch (IllegalArgumentException e) {
            createConstraintMessage(context, String.format(PROTOCOL_ADDRESS_FORMAT_INVALID, e.getMessage()));
            return false;
        }
    }

    private boolean isValidUriTemplate(String address, ConstraintValidatorContext context) {
        try {
            UriTemplate template = UriTemplate.fromTemplate(address);

            if (isInvalidHost(template)) {
                createConstraintMessage(context, "Endpoint contains invalid chars in host name. Underscore is one of them.");
                return false;
            }

        } catch (Exception e) {
            createConstraintMessage(context, String.format(PROTOCOL_ADDRESS_FORMAT_INVALID, e.getMessage()));
            return false;
        }

        return true;
    }

    private void createConstraintMessage(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addConstraintViolation()
                .disableDefaultConstraintViolation();
    }

    private boolean isInvalidHost(UriTemplate template) {
        Map<String, Object> uriKeysWithEmptyValues = asList(template.getVariables()).stream().collect(toMap(identity(), v -> "empty"));

        //check if host is null due to bug in jdk http://bugs.java.com/view_bug.do?bug_id=6587184
        return URI.create(template.expand(uriKeysWithEmptyValues)).getHost() == null;
    }
}
