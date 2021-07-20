package pl.allegro.tech.hermes.frontend.services;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.domain.readiness.ReadinessRepository;

@Singleton
public class ReadinessCheckService {
    private static final Logger logger = LoggerFactory.getLogger(ReadinessCheckService.class);

    private final List<ReadinessRepository> readinessRepositories;

    @Inject
    public ReadinessCheckService(List<ReadinessRepository> readinessRepositories) {
        this.readinessRepositories = readinessRepositories;
    }

    public boolean isReady() {
        try {
            return readinessRepositories.stream().allMatch(ReadinessRepository::isReady);
        } catch (Exception ex) {
            logger.error("Error while reading readiness status...", ex);
            return true;
        }
    }
}
