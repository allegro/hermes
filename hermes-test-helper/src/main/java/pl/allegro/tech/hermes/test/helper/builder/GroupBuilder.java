package pl.allegro.tech.hermes.test.helper.builder;

import pl.allegro.tech.hermes.api.Group;

public class GroupBuilder {

    private final String groupName;

    private GroupBuilder(String name) {
        this.groupName = name;
    }

    public static GroupBuilder group(String name) {
        return new GroupBuilder(name);
    }

    public Group build() {
        return new Group(groupName);
    }
}
