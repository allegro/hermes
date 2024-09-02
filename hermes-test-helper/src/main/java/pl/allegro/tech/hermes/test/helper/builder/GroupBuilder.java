package pl.allegro.tech.hermes.test.helper.builder;

import java.util.concurrent.atomic.AtomicLong;
import pl.allegro.tech.hermes.api.Group;

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
    return groupWithRandomNameEndedWith("");
  }

  public static GroupBuilder groupWithRandomNameEndedWith(String suffix) {
    return group(
        GroupBuilder.class.getSimpleName() + "Group" + sequence.incrementAndGet() + suffix);
  }

  public static GroupBuilder groupWithRandomNameContaining(String string) {
    return group(
        GroupBuilder.class.getSimpleName() + "Group" + string + sequence.incrementAndGet());
  }
}
