package pl.allegro.tech.hermes.management.domain;

import pl.allegro.tech.hermes.api.Anonymizable;
import pl.allegro.tech.hermes.api.PatchData;

public interface Auditor {

  default void beforeObjectCreation(String username, Object createdObject) {}

  default void beforeObjectCreation(String username, Anonymizable createdObject) {
    beforeObjectCreation(username, (Object) createdObject.anonymize());
  }

  default void beforeObjectRemoval(
      String username, String removedObjectType, String removedObjectName) {}

  default void beforeObjectUpdate(
      String username, String objectClassName, Object objectName, PatchData patchData) {}

  default void objectCreated(String username, Object createdObject) {}

  default void objectCreated(String username, Anonymizable createdObject) {
    objectCreated(username, (Object) createdObject.anonymize());
  }

  default void objectRemoved(String username, Object removedObject) {}

  default void objectRemoved(String username, Anonymizable removedObject) {
    objectRemoved(username, (Object) removedObject.anonymize());
  }

  default void objectUpdated(String username, Object oldObject, Object newObject) {}

  default void objectUpdated(String username, Anonymizable oldObject, Anonymizable newObject) {
    objectUpdated(username, (Object) oldObject.anonymize(), (Object) newObject.anonymize());
  }
}
