package pl.allegro.tech.hermes.consumers.supervisor.workTracking;

import com.google.common.collect.ImmutableMap;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_WORKLOAD_ALGORITHM;

public class SupervisorControllerFactory implements Factory<SupervisorController> {
    private final ConfigFactory configs;
    private final Map<String, Provider<SupervisorController>> availableImplementations;

    @Inject
    public SupervisorControllerFactory(SubscriptionsCache subscriptionsCache,
                                       WorkTracker workTracker,
                                       ConsumersSupervisor supervisor, ConfigFactory configs) {
        this.configs = configs;
        this.availableImplementations = ImmutableMap.of(
                "legacy.mirror", () -> new LegacyMirroringSupervisorController(supervisor, subscriptionsCache),
                "mirror", () -> new MirroringSupervisorController(supervisor, subscriptionsCache, workTracker));
    }

    @Override
    public SupervisorController provide() {
        return availableImplementations.get(configs.getStringProperty(CONSUMER_WORKLOAD_ALGORITHM)).get();
    }

    @Override
    public void dispose(SupervisorController instance) {

    }
}
