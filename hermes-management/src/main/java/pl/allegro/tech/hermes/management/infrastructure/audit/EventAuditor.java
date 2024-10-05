package pl.allegro.tech.hermes.management.infrastructure.audit;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.management.domain.Auditor;

public class EventAuditor implements Auditor {

  private static final Logger logger = LoggerFactory.getLogger(EventAuditor.class);

  private final Javers javers;

  private final RestTemplate restTemplate;

  private final String eventDestination;

  private final ObjectMapper objectMapper;

  public EventAuditor(
      Javers javers,
      RestTemplate restTemplate,
      String eventDestination,
      ObjectMapper objectMapper) {
    this.javers = checkNotNull(javers);
    this.restTemplate = checkNotNull(restTemplate);
    this.eventDestination = eventDestination;
    this.objectMapper = objectMapper;
  }

  @Override
  public void beforeObjectCreation(String username, Object createdObject) {
    ignoringExceptions(
        () -> {
          String createdObjectToString = objectMapper.writeValueAsString(createdObject);
          AuditEvent event =
              new AuditEvent(
                  AuditEventType.BEFORE_CREATION,
                  createdObjectToString,
                  createdObject.getClass().getSimpleName(),
                  createdObject.toString(),
                  username);
          restTemplate.postForObject(eventDestination, event, Void.class);
        });
  }

  @Override
  public void beforeObjectRemoval(
      String username, String removedObjectType, String removedObjectName) {
    ignoringExceptions(
        () -> {
          AuditEvent event =
              new AuditEvent(
                  AuditEventType.BEFORE_REMOVAL,
                  removedObjectName,
                  removedObjectType,
                  removedObjectName,
                  username);
          restTemplate.postForObject(eventDestination, event, Void.class);
        });
  }

  @Override
  public void beforeObjectUpdate(
      String username, String objectClassName, Object objectName, PatchData patchData) {
    ignoringExceptions(
        () -> {
          String patchDataToString = objectMapper.writeValueAsString(patchData);
          AuditEvent event =
              new AuditEvent(
                  AuditEventType.BEFORE_UPDATE,
                  patchDataToString,
                  patchData.getClass().getSimpleName(),
                  objectName.toString(),
                  username);
          restTemplate.postForObject(eventDestination, event, Void.class);
        });
  }

  @Override
  public void objectCreated(String username, Object createdObject) {
    ignoringExceptions(
        () -> {
          String createdObjectToString = objectMapper.writeValueAsString(createdObject);
          AuditEvent event =
              new AuditEvent(
                  AuditEventType.CREATED,
                  createdObjectToString,
                  createdObject.getClass().getSimpleName(),
                  createdObject.toString(),
                  username);
          restTemplate.postForObject(eventDestination, event, Void.class);
        });
  }

  @Override
  public void objectRemoved(String username, Object removedObject) {
    ignoringExceptions(
        () -> {
          String removedObjectToString = objectMapper.writeValueAsString(removedObject);
          AuditEvent event =
              new AuditEvent(
                  AuditEventType.REMOVED,
                  removedObjectToString,
                  removedObject.getClass().getSimpleName(),
                  removedObject.toString(),
                  username);
          restTemplate.postForObject(eventDestination, event, Void.class);
        });
  }

  @Override
  public void objectUpdated(String username, Object oldObject, Object newObject) {
    ignoringExceptions(
        () -> {
          Diff diff = javers.compare(oldObject, newObject);
          AuditEvent event =
              new AuditEvent(
                  AuditEventType.UPDATED,
                  diff.toString(),
                  oldObject.getClass().getSimpleName(),
                  oldObject.toString(),
                  username);
          restTemplate.postForObject(eventDestination, event, Void.class);
        });
  }

  private void ignoringExceptions(Wrapped wrapped) {
    try {
      wrapped.execute();
    } catch (Exception e) {
      logger.info("Audit event emission failed.", e);
    }
  }

  @FunctionalInterface
  private interface Wrapped {
    void execute() throws Exception;
  }
}
