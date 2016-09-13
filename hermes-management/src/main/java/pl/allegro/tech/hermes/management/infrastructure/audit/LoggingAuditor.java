package pl.allegro.tech.hermes.management.infrastructure.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javers.core.Javers;
import org.javers.core.diff.Diff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.management.domain.Auditor;

import static com.google.common.base.Preconditions.checkNotNull;

public class LoggingAuditor implements Auditor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAuditor.class);

    private final Javers javers;
    private final ObjectMapper objectMapper;

    public LoggingAuditor(Javers javers, ObjectMapper objectMapper) {
        this.javers = checkNotNull(javers);
        this.objectMapper = checkNotNull(objectMapper);
    }

    @Override
    public void objectCreated(String username, Object createdObject) {
        ignoringExceptions(() ->
            logger.info("User {} has created new object {}.", username, objectMapper.writeValueAsString(createdObject)));
    }

    @Override
    public void objectRemoved(String username, String removedObjectName) {
        logger.info("User {} has removed object {}.", username, removedObjectName);
    }

    @Override
    public void objectUpdated(String username, Object oldObject, Object newObject) {
        ignoringExceptions(() -> {
            Diff diff = javers.compare(oldObject, newObject);
            logger.info("User {} has updated object {}. {}", username, objectMapper.writeValueAsString(oldObject), diff);
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
