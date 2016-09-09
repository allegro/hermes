package pl.allegro.tech.hermes.management.domain;

public interface Auditor {
    void objectCreated(String username, Object createdObject);
    void objectRemoved(String username, String removedObjectName);
    void objectUpdated(String username, Object oldObject, Object newObject);
}
