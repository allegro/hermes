package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.net.URI;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.allegro.tech.hermes.api.jackson.EndpointAddressDeserializer;
import pl.allegro.tech.hermes.api.jackson.EndpointAddressSerializer;

@JsonDeserialize(using = EndpointAddressDeserializer.class)
@JsonSerialize(using = EndpointAddressSerializer.class)
public class EndpointAddress implements Anonymizable {

  private static final String ANONYMIZED_PASSWORD = "*****";

  private static final Pattern URL_PATTERN =
      Pattern.compile("([a-zA-Z0-9]*)://(([a-zA-Z0-9\\.\\~\\-\\_]*):(.*)@)?(.*)");

  private static final int PROTOCOL_GROUP = 1;

  private static final int ADDRESS_GROUP = 5;

  private static final int USER_INFO_GROUP = 2;

  private static final int USERNAME_GROUP = 3;

  private static final int PASSWORD_GROUP = 4;

  private final boolean containsCredentials;

  private final String protocol;

  private final String username;

  private final String password;

  private final String endpoint;

  private final String rawEndpoint;

  public EndpointAddress(String endpoint) {
    this.rawEndpoint = endpoint;

    Matcher matcher = URL_PATTERN.matcher(endpoint);
    if (matcher.matches()) {
      this.protocol = matcher.group(PROTOCOL_GROUP);
      this.containsCredentials = !Strings.isNullOrEmpty(matcher.group(USER_INFO_GROUP));

      this.username = containsCredentials ? matcher.group(USERNAME_GROUP) : null;
      this.password = containsCredentials ? matcher.group(PASSWORD_GROUP) : null;

      this.endpoint =
          containsCredentials ? protocol + "://" + matcher.group(ADDRESS_GROUP) : endpoint;
    } else {
      this.protocol = null;
      this.containsCredentials = false;
      this.username = null;
      this.password = null;
      this.endpoint = endpoint;
    }
  }

  private EndpointAddress(String protocol, String endpoint, String username) {
    this.protocol = protocol;
    this.endpoint = endpoint;
    this.containsCredentials = true;
    this.username = username;
    this.password = ANONYMIZED_PASSWORD;

    this.rawEndpoint =
        protocol + "://" + username + ":" + password + "@" + endpoint.replace(protocol + "://", "");
  }

  public static EndpointAddress of(String endpoint) {
    return new EndpointAddress(endpoint);
  }

  public static EndpointAddress of(URI endpoint) {
    return new EndpointAddress(endpoint.toString());
  }

  public static String extractProtocolFromAddress(String endpoint) {
    Preconditions.checkArgument(endpoint.indexOf(':') != -1);

    return endpoint.substring(0, endpoint.indexOf(':'));
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

  public String getProtocol() {
    return protocol;
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawEndpoint);
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
    return Objects.equals(this.rawEndpoint, other.rawEndpoint);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("endpoint", endpoint).toString();
  }

  public boolean containsCredentials() {
    return containsCredentials;
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }

  public EndpointAddress anonymize() {
    if (containsCredentials) {
      return new EndpointAddress(protocol, endpoint, username);
    }
    return this;
  }
}
