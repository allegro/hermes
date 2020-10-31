package pl.allegro.tech.hermes.management.infrastructure.audit;

import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.domain.Auditor;
import pl.allegro.tech.hermes.management.infrastructure.audit.pojo.AuditEvent;
import pl.allegro.tech.hermes.management.infrastructure.audit.pojo.AuditEventType;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventAuditor implements Auditor {

    private static final Logger logger = LoggerFactory.getLogger(EventAuditor.class);

    private final Javers javers;

    private final RestTemplate restTemplate;

    private final String eventDestination;

    public EventAuditor(Javers javers, RestTemplate restTemplate, String eventDestination) {
        this.javers = checkNotNull(javers);
        this.restTemplate = checkNotNull(restTemplate);
        this.eventDestination = eventDestination;
    }


    @Override
    public void objectCreated(String username, Object createdObject) {
        ignoringExceptions(() -> {
            AuditEvent event = new AuditEvent(AuditEventType.CREATED, createdObject, username);
            restTemplate.postForObject(eventDestination, event, Void.class);
        });
    }

    @Override
    public void objectRemoved(String username, String removedObjectType, String removedObjectName) {
        ignoringExceptions(() -> {
            AuditEvent event = new AuditEvent(AuditEventType.REMOVED, removedObjectName, Class.forName(removedObjectType), username);
            restTemplate.postForObject(eventDestination, event, Void.class);
        });
    }

    @Override
    public void objectUpdated(String username, Object oldObject, Object newObject) {
        ignoringExceptions(() -> {
            Diff diff = javers.compare(oldObject, newObject);
            AuditEvent event = new AuditEvent(AuditEventType.UPDATED, diff, newObject.getClass(), username);
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
