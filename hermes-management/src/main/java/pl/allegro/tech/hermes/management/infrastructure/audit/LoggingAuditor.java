package pl.allegro.tech.hermes.management.infrastructure.audit;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.management.domain.Auditor;

public class LoggingAuditor implements Auditor {

  private static final Logger logger = LoggerFactory.getLogger(LoggingAuditor.class);

  private final Javers javers;
  private final ObjectMapper objectMapper;

  public LoggingAuditor(Javers javers, ObjectMapper objectMapper) {
    this.javers = checkNotNull(javers);
    this.objectMapper = checkNotNull(objectMapper);
  }

  @Override
  public void beforeObjectCreation(String username, Object createdObject) {
    ignoringExceptions(
        () ->
            logger.info(
                "User {} tries creating new {} {}.",
                username,
                createdObject.getClass().getSimpleName(),
                objectMapper.writeValueAsString(createdObject)));
  }

  @Override
  public void beforeObjectRemoval(
      String username, String removedObjectType, String removedObjectName) {
    logger.info("User {} tries removing {} {}.", username, removedObjectType, removedObjectName);
  }

  @Override
  public void beforeObjectUpdate(
      String username, String objectClassName, Object objectName, PatchData patchData) {
    ignoringExceptions(
        () -> {
          logger.info(
              "User {} tries updating {} {}. {}", username, objectClassName, objectName, patchData);
        });
  }

  @Override
  public void objectCreated(String username, Object createdObject) {
    ignoringExceptions(
        () ->
            logger.info(
                "User {} has created new {} {}.",
                username,
                createdObject.getClass().getSimpleName(),
                objectMapper.writeValueAsString(createdObject)));
  }

  @Override
  public void objectRemoved(String username, Object removedObject) {
    ignoringExceptions(
        () ->
            logger.info(
                "User {} has removed {} {}.",
                username,
                removedObject.getClass().getSimpleName(),
                objectMapper.writeValueAsString(removedObject)));
  }

  @Override
  public void objectUpdated(String username, Object oldObject, Object newObject) {
    ignoringExceptions(
        () -> {
          Diff diff = javers.compare(oldObject, newObject);
          logger.info(
              "User {} has updated {} {}. {}",
              username,
              oldObject.getClass().getSimpleName(),
              objectMapper.writeValueAsString(oldObject),
              diff);
        });
  }

  private void ignoringExceptions(Wrapped wrapped) {
    try {
      wrapped.execute();
    } catch (Exception e) {
      logger.info("Audit log failed {}.", e);
    }
  }

  @FunctionalInterface
  private interface Wrapped {
    void execute() throws Exception;
  }
}
