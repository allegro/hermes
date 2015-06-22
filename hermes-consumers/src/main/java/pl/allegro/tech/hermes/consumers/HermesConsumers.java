package pl.allegro.tech.hermes.consumers;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.component.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.consumers.consumer.health.HealthCheckServer;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderProviders;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.supervisor.ConsumersSupervisor;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class HermesConsumers {

    private static final Logger logger = LoggerFactory.getLogger(HermesConsumers.class);

    private final HooksHandler hooksHandler;
    private final ConsumersSupervisor consumersSupervisor;
    private final HealthCheckServer healthCheckServer;
    private final Trackers trackers;
    private final List<Function<ServiceLocator, LogRepository>> logRepositories;
    private MultiMap<String, Supplier<ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers;
    private final MessageSenderProviders messageSendersProviders;
    private final ServiceLocator serviceLocator;

    public static void main(String... args) {
        consumers().build().start();
    }

    HermesConsumers(HooksHandler hooksHandler,
                    List<Binder> binders,
                    MultiMap<String, Supplier<ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers,
                    List<Function<ServiceLocator, LogRepository>> logRepositories) {

        this.hooksHandler = hooksHandler;
        this.messageSenderProvidersSuppliers = messageSenderProvidersSuppliers;
        this.logRepositories = logRepositories;

        serviceLocator = createDIContainer(binders);

        trackers = serviceLocator.getService(Trackers.class);
        consumersSupervisor = serviceLocator.getService(ConsumersSupervisor.class);
        healthCheckServer = serviceLocator.getService(HealthCheckServer.class);
        messageSendersProviders = serviceLocator.getService(MessageSenderProviders.class);

        hooksHandler.addShutdownHook(() -> {
            try {
                healthCheckServer.stop();
                consumersSupervisor.shutdown();
                serviceLocator.shutdown();
            } catch (InterruptedException e) {
                logger.error("Exception while shutdown Hermes Consumers", e);
            }
        });
    }

    public void start() {
        try {
            logRepositories.forEach(serviceLocatorLogRepositoryFunction ->
                    trackers.add(serviceLocatorLogRepositoryFunction.apply(serviceLocator)));

            messageSenderProvidersSuppliers.entrySet().stream().forEach(entry -> {
                entry.getValue().stream().forEach( supplier -> {
                    messageSendersProviders.put(entry.getKey(), supplier.get());
                });
            });

            consumersSupervisor.start();
            healthCheckServer.start();
            hooksHandler.startup();
        } catch (Exception e) {
            logger.error("Exception while starting Hermes Consumers", e);
        }
    }

    public void stop() {
        hooksHandler.shutdown();
    }

    private ServiceLocator createDIContainer(List<Binder> binders) {
        String uniqueName = "HermesConsumersLocator" + UUID.randomUUID();
        return ServiceLocatorUtilities.bind(uniqueName, binders.toArray(new Binder[binders.size()]));
    }

    public <T> T getService(Class<T> clazz) {
        return serviceLocator.getService(clazz);
    }

    public <T> T getService(Class<T> clazz, String name) {
        return serviceLocator.getService(clazz, name);
    }

    public static HermesConsumersBuilder consumers() {
        return new HermesConsumersBuilder();
    }

}
