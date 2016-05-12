package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import pl.allegro.tech.hermes.api.constraints.ValidAddress;
import pl.allegro.tech.hermes.api.jackson.EndpointAddressDeserializer;
import pl.allegro.tech.hermes.api.jackson.EndpointAddressSerializer;

import java.net.URI;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonDeserialize(using = EndpointAddressDeserializer.class)
@JsonSerialize(using = EndpointAddressSerializer.class)
public class EndpointAddress {

    private static final Pattern URL_PATTERN = Pattern.compile("([a-zA-Z0-9]*)://(.*)");

    private static final int PROTOCOL_GROUP = 1;

    private static final int ADDRESS_GROUP = 2;

    private final String protocol;

    @ValidAddress(message = "Endpoint address is invalid")
    private final String endpoint;

    private final String rawEndpoint;

    public EndpointAddress(String endpoint) {
        this.rawEndpoint = endpoint;

        Matcher matcher = URL_PATTERN.matcher(endpoint);
        if(matcher.matches()) {
            this.protocol = matcher.group(PROTOCOL_GROUP);

            this.endpoint = endpoint;
        }
        else {
            this.protocol = null;

            this.endpoint = endpoint;
        }
    }

    private EndpointAddress(String protocol, String endpoint) {
        this.protocol = protocol;
        this.endpoint = endpoint;
        this.rawEndpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRawEndpoint() {
        return rawEndpoint;
    }

    public URI getUri() {
        return URI.create(endpoint);
    }

    public static EndpointAddress of(String endpoint) {
        return new EndpointAddress(endpoint);
    }

    public static EndpointAddress of(URI endpoint) {
        return new EndpointAddress(endpoint.toString());
    }

    public String getProtocol() {
        return protocol;
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
        Preconditions.checkArgument(endpoint.indexOf(':') != -1);

        return endpoint.substring(0, endpoint.indexOf(':'));
    }
}
