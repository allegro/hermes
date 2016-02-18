package pl.allegro.tech.hermes.test.helper.builder;

import pl.allegro.tech.hermes.api.Group;

public class GroupBuilder {

    private final String groupName;

    private String technicalOwner = "owner";

    private String supportTeam = "team";

    private String contact = "contact@example.com";

    private GroupBuilder(String name) {
        this.groupName = name;
    }

    public static GroupBuilder group(String name) {
        return new GroupBuilder(name);
    }

    public Group build() {
        return new Group(groupName, technicalOwner, supportTeam, contact);
    }

    public GroupBuilder withTechnicalOwner(String technicalOwner) {
        this.technicalOwner = technicalOwner;
        return this;
    }

    public GroupBuilder withSupportTeam(String supportTeam) {
        this.supportTeam = supportTeam;
        return this;
    }

    public GroupBuilder withContact(String contact) {
        this.contact = contact;
        return this;
    }
}
