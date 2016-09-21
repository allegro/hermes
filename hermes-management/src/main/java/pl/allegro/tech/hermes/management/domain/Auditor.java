package pl.allegro.tech.hermes.management.domain;

public interface Auditor<T extends Object> {
    default void objectCreated(String username, T createdObject) {
    }

    default void objectRemoved(String username, String removedObjectType, String removedObjectName) {
    }

    default void objectUpdated(String username, T oldObject, T newObject) {
    }

    static Auditor noOpAuditor() {
        return new NoOpAuditor();
    }

    class NoOpAuditor implements Auditor {
        private NoOpAuditor() {
        }
    }
}
