package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import pl.allegro.tech.hermes.api.constraints.Names;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Objects;

public class Group {

    @NotNull
    @Pattern(regexp = Names.ALLOWED_NAME_REGEX)
    private String groupName;

    @NotNull
    private String technicalOwner;

    @NotNull
    private String supportTeam;

    @NotNull
    private String contact;

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
}
