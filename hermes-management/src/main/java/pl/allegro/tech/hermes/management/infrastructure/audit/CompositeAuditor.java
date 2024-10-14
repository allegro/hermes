package pl.allegro.tech.hermes.management.infrastructure.audit;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.management.domain.Auditor;

public class CompositeAuditor implements Auditor {

  private final Collection<Auditor> auditors;

  public CompositeAuditor(Collection<Auditor> auditors) {
    this.auditors = checkNotNull(auditors);
  }

  @Override
  public void beforeObjectCreation(String username, Object createdObject) {
    auditors.forEach(auditor -> auditor.beforeObjectCreation(username, createdObject));
  }

  @Override
  public void beforeObjectRemoval(
      String username, String removedObjectType, String removedObjectName) {
    auditors.forEach(
        auditor -> auditor.beforeObjectRemoval(username, removedObjectType, removedObjectName));
  }

  @Override
  public void beforeObjectUpdate(
      String username, String objectClassName, Object objectName, PatchData patchData) {
    auditors.forEach(
        auditor -> auditor.beforeObjectUpdate(username, objectClassName, objectName, patchData));
  }

  @Override
  public void objectCreated(String username, Object createdObject) {
    auditors.forEach(auditor -> auditor.objectCreated(username, createdObject));
  }

  @Override
  public void objectRemoved(String username, Object removedObject) {
    auditors.forEach(auditor -> auditor.objectRemoved(username, removedObject));
  }

  @Override
  public void objectUpdated(String username, Object oldObject, Object newObject) {
    auditors.forEach(auditor -> auditor.objectUpdated(username, oldObject, newObject));
  }
}
