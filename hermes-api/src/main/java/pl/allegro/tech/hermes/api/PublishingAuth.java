package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PublishingAuth {

    private final List<String> publishers;
    private final boolean enabled;
    private final boolean unauthorisedAccessEnabled;

    @JsonCreator
    public PublishingAuth(@JsonProperty("publishers") List<String> publishers,
                          @JsonProperty("enabled") boolean enabled,
                          @JsonProperty("unauthorisedAccessEnabled") boolean unauthorisedAccessEnabled) {

        this.publishers = publishers;
        this.enabled = enabled;
        this.unauthorisedAccessEnabled = unauthorisedAccessEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUnauthorisedAccessEnabled() {
        return unauthorisedAccessEnabled;
    }

    public boolean hasPermission(String publisher) {
        return publishers.contains(publisher);
    }

    public List<String> getPublishers() {
        return publishers;
    }

    public static PublishingAuth disabled() {
        return new PublishingAuth(new ArrayList<>(), false, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PublishingAuth that = (PublishingAuth) o;
        return enabled == that.enabled
                && unauthorisedAccessEnabled == that.unauthorisedAccessEnabled
                && Objects.equals(publishers, that.publishers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publishers, enabled, unauthorisedAccessEnabled);
    }
}
