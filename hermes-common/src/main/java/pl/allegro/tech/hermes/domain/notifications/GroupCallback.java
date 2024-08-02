package pl.allegro.tech.hermes.domain.notifications;

import pl.allegro.tech.hermes.api.Group;

public interface GroupCallback {

    default void onGroupCreated(Group group) {
    }

    default void onGroupRemoved(Group group) {
    }

    default void onGroupChanged(Group group) {
    }
}
