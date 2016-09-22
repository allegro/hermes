package pl.allegro.tech.hermes.management.domain;

import pl.allegro.tech.hermes.api.Anonymizable;

public interface Auditor {

    default void objectCreated(String username, Object createdObject) {
    }

    default void objectCreated(String username, Anonymizable createdObject) {
        objectCreated(username, (Object) createdObject.anonymize());
    }

    default void objectRemoved(String username, String removedObjectType, String removedObjectName) {
    }

    default void objectUpdated(String username, Object oldObject, Object newObject) {
    }

    default void objectUpdated(String username, Anonymizable oldObject, Anonymizable newObject) {
        objectUpdated(username, (Object) oldObject.anonymize(), (Object) newObject.anonymize());
    }

    static Auditor noOpAuditor() {
        return new NoOpAuditor();
    }

    class NoOpAuditor implements Auditor {
        private NoOpAuditor() {
        }
    }
}
