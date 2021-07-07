package pl.allegro.tech.hermes.frontend.server;

import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;

public class KafkaReadOnlyReadinessRepository implements ReadinessRepository {
    private static final Logger logger = LoggerFactory.getLogger(KafkaReadOnlyReadinessRepository.class);
    private final KafkaHealthChecker kafkaHealthChecker;

    @Inject
    public KafkaReadOnlyReadinessRepository(KafkaHealthChecker kafkaHealthChecker) {
        this.kafkaHealthChecker = kafkaHealthChecker;
    }

    @Override
    public boolean isReady() {
        try {
            return kafkaHealthChecker.waitForKafkaReadiness().isSuccess();
        } catch (Exception ex) {
            logger.warn("Kafka not ready...", ex);
            return false;
        }
    }

    @Override
    public void setReadiness(boolean isReady) {
    }
}
