package pl.allegro.tech.hermes.test.helper.builder;

import pl.allegro.tech.hermes.api.Group;

import java.util.concurrent.atomic.AtomicLong;

public class GroupBuilder {

    private static final AtomicLong sequence = new AtomicLong();

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

    public static GroupBuilder groupWithRandomName() {
        return group(GroupBuilder.class.getSimpleName() + "Group" + sequence.incrementAndGet());
    }
}
