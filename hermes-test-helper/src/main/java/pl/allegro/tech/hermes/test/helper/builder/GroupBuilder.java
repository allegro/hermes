package pl.allegro.tech.hermes.test.helper.builder;

import pl.allegro.tech.hermes.api.Group;

import java.util.UUID;

public class GroupBuilder {

    private final String groupName;

    private GroupBuilder(String name) {
        this.groupName = name;
    }

    public static GroupBuilder group(String name) {
        return new GroupBuilder(name);
    }

    public static GroupBuilder randomGroup(String groupNamePrefix) {
        return group(groupNamePrefix + "-" + UUID.randomUUID());
    }

    public Group build() {
        return new Group(groupName);
    }
}
