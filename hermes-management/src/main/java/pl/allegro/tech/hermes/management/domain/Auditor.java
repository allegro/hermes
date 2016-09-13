package pl.allegro.tech.hermes.management.domain;

public interface Auditor {
    default void objectCreated(String username, Object createdObject) {}
    default void objectRemoved(String username, String removedObjectName) {}
    default void objectUpdated(String username, Object oldObject, Object newObject) {}

    static Auditor noopAuditor() {
        return new NoOpAuditor();
    }

    class NoOpAuditor implements Auditor {
        private NoOpAuditor() {}
    }
}
