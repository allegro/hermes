package pl.allegro.tech.hermes.management.infrastructure.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javers.core.Javers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.infrastructure.audit.pojo.AuditEvent;
import pl.allegro.tech.hermes.management.infrastructure.audit.pojo.AuditEventType;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventAuditor implements Auditor {

    private static final Logger logger = LoggerFactory.getLogger(EventAuditor.class);

    private final Javers javers;

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    public EventAuditor(Javers javers, WebClient webClient, ObjectMapper objectMapper) {
        this.javers = checkNotNull(javers);
        this.webClient = checkNotNull(webClient);
        this.objectMapper = checkNotNull(objectMapper);
    }

    @Override
    public void beforeObjectCreation(String username, Object createdObject) {
        ignoringExceptions(() -> {

        });
    }

    @Override
    public void beforeObjectRemoval(String username, String removedObjectType, String removedObjectName) {
        logger.info("User {} tries removing {} {}.", username, removedObjectType, removedObjectName);
    }

    @Override
    public void beforeObjectUpdate(String username, String objectClassName, Object objectName, PatchData patchData) {
        ignoringExceptions(() -> {
        });
    }

    @Override
    public void objectCreated(String username, Object createdObject) {
        ignoringExceptions(() -> {
            AuditEvent event = new AuditEvent(AuditEventType.CREATED, createdObject);
            webClient.post()
                    .body(BodyInserters.fromObject(event))
                    .exchange()
                    .doOnNext(clientResponse -> logger.info("Emitted creation event"));
        });
    }

    @Override
    public void objectRemoved(String username, String removedObjectType, String removedObjectName) {
        logger.info("User {} has removed {} {}.", username, removedObjectType, removedObjectName);
    }

    @Override
    public void objectUpdated(String username, Object oldObject, Object newObject) {
        ignoringExceptions(() -> {
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
