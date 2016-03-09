package pl.allegro.tech.hermes.domain.group;

import pl.allegro.tech.hermes.api.Group;

import java.util.List;

public interface GroupRepository {

    boolean groupExists(String groupName);

    void ensureGroupExists(String groupName);

    void createGroup(Group group);

    void updateGroup(Group group);

    void removeGroup(String groupName);

    List<String> listGroupNames();

    List<Group> listGroups();

    Group getGroupDetails(String groupName);

}
