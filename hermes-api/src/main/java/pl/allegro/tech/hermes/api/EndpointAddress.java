package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.net.URI;
import pl.allegro.tech.hermes.api.constraints.ValidAddress;
import pl.allegro.tech.hermes.api.jackson.EndpointAddressDeserializer;
import pl.allegro.tech.hermes.api.jackson.EndpointAddressSerializer;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonDeserialize(using = EndpointAddressDeserializer.class)
@JsonSerialize(using = EndpointAddressSerializer.class)
public class EndpointAddress {

    private static final String ANONYMIZED_PASSWORD = "*****";

    private static final Pattern URL_WITH_CREDENTIALS_PATTERN = Pattern.compile("([a-zA-Z0-9_]*://)([a-zA-Z0-9_\\.]*:)(.*)(@.*)");

    @ValidAddress(message = "Endpoint address is invalid")
    private final String endpoint;

    public EndpointAddress(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public static EndpointAddress of(String endpoint) {
        return new EndpointAddress(endpoint);
    }

    public static EndpointAddress of(URI endpoint) {
        return new EndpointAddress(endpoint.toString());
    }

    public String getProtocol() {
        return extractProtocolFromAddress(endpoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EndpointAddress other = (EndpointAddress) obj;
        return Objects.equals(this.endpoint, other.endpoint);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("endpoint", endpoint)
                .toString();
    }

    public static String extractProtocolFromAddress(String endpoint) {
        return endpoint.substring(0, endpoint.indexOf(':'));
    }

    public boolean containsCredentials() {
        return URL_WITH_CREDENTIALS_PATTERN.matcher(endpoint).matches();
    }

    public EndpointAddress anonymizePassword() {
        Matcher m = URL_WITH_CREDENTIALS_PATTERN.matcher(endpoint);
        if (m.matches()) {
            return new EndpointAddress(m.group(1) + m.group(2) + ANONYMIZED_PASSWORD + m.group(4));
        }
        return this;
    }
}
