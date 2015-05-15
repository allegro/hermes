package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.constraints.Names;
import pl.allegro.tech.hermes.api.helpers.Patch;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Objects;

public class Group {

    @NotNull
    @Pattern(regexp = Names.ALLOWED_NAME_REGEX)
    private String groupName;
    private String technicalOwner;
    private String supportTeam;
    private String contact;

    private Group() { }

    @JsonCreator
    public Group(@JsonProperty("groupName") String groupName,
            @JsonProperty("technicalOwner") String technicalOwner,
            @JsonProperty("supportTeam") String supportTeam,
            @JsonProperty("contact") String contact
    ) {
        this.groupName = groupName;
        this.technicalOwner = technicalOwner;
        this.supportTeam = supportTeam;
        this.contact = contact;
    }

    public static Group from(String groupName) {
        return new Group(groupName, null, null, null);
    }

    public String getGroupName() {
        return groupName;
    }

    public String getTechnicalOwner() {
        return technicalOwner;
    }

    public String getSupportTeam() {
        return supportTeam;
    }

    public String getContact() {
        return contact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Group)) {
            return false;
        }
        Group group = (Group) o;

        return Objects.equals(this.getGroupName(), group.getGroupName())
                && Objects.equals(this.getTechnicalOwner(), group.getTechnicalOwner())
                && Objects.equals(this.getSupportTeam(), group.getSupportTeam());
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName, technicalOwner, supportTeam);
    }

    public static final class Builder {
        private Group group;

        private Builder() {
            group = new Group();
        }

        public Builder withGroupName(String groupName) {
            group.groupName = groupName;
            return this;
        }

        public Builder withTechnicalOwner(String technicalOwner) {
            group.technicalOwner = technicalOwner;
            return this;
        }

        public Builder withSupportTeam(String supportTeam) {
            group.supportTeam = supportTeam;
            return this;
        }

        public Builder withContact(String contact) {
            group.contact = contact;
            return this;
        }

        public Builder applyPatch(Object update) {
            if (update != null) {
                group = Patch.apply(group, update);
            }
            return this;
        }

        public static Builder group() {
            return new Builder();
        }

        public Group build() {
            return group;
        }
    }

}
