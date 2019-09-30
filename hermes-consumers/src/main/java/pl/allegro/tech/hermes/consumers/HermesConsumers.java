package pl.allegro.tech.hermes.consumers;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.component.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.common.hook.FlushLogsShutdownHook;
import pl.allegro.tech.hermes.common.hook.HooksHandler;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsWorkloadReporter;
import pl.allegro.tech.hermes.consumers.health.ConsumerHttpServer;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SubscriptionAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class HermesConsumers {

    private static final Logger logger = LoggerFactory.getLogger(HermesConsumers.class);

    private final HooksHandler hooksHandler;
    private final ConsumerHttpServer consumerHttpServer;
    private final Trackers trackers;
    private final List<Function<ServiceLocator, LogRepository>> logRepositories;
    private final MultiMap<String, Function<ServiceLocator, ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers;
    private final MessageSenderFactory messageSenderFactory;
    private final ServiceLocator serviceLocator;

    private final SupervisorController supervisorController;
    private final MaxRateSupervisor maxRateSupervisor;
    private final SubscriptionAssignmentCache assignmentCache;
    private final OAuthClient oAuthHttpClient;
    private final HttpClientsWorkloadReporter httpClientsWorkloadReporter;

    public static void main(String... args) {
        consumers().build().start();
    }

    HermesConsumers(HooksHandler hooksHandler,
                    List<Binder> binders,
                    MultiMap<String, Function<ServiceLocator, ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers,
                    List<Function<ServiceLocator, LogRepository>> logRepositories, boolean flushLogsShutdownHookEnabled) {

        this.hooksHandler = hooksHandler;
        this.messageSenderProvidersSuppliers = messageSenderProvidersSuppliers;
        this.logRepositories = logRepositories;

        serviceLocator = createDIContainer(binders);

        trackers = serviceLocator.getService(Trackers.class);
        consumerHttpServer = serviceLocator.getService(ConsumerHttpServer.class);
        messageSenderFactory = serviceLocator.getService(MessageSenderFactory.class);

        supervisorController = serviceLocator.getService(SupervisorController.class);
        maxRateSupervisor = serviceLocator.getService(MaxRateSupervisor.class);
        assignmentCache = serviceLocator.getService(SubscriptionAssignmentCache.class);
        oAuthHttpClient = serviceLocator.getService(OAuthClient.class);
        httpClientsWorkloadReporter = serviceLocator.getService(HttpClientsWorkloadReporter.class);

        hooksHandler.addShutdownHook((s) -> {
            try {
                consumerHttpServer.stop();
                maxRateSupervisor.stop();
                assignmentCache.stop();
                oAuthHttpClient.stop();
                supervisorController.shutdown();
                s.shutdown();
            } catch (Exception e) {
                logger.error("Exception while shutdown Hermes Consumers", e);
            }
        });
        if (flushLogsShutdownHookEnabled) {
            hooksHandler.addShutdownHook(new FlushLogsShutdownHook());
        }
    }

    public void start() {
        try {
            oAuthHttpClient.start();
            logRepositories.forEach(serviceLocatorLogRepositoryFunction ->
                    trackers.add(serviceLocatorLogRepositoryFunction.apply(serviceLocator)));

            messageSenderProvidersSuppliers.entrySet().stream().forEach(entry ->
                    entry.getValue().stream().forEach(supplier ->
                            messageSenderFactory.addSupportedProtocol(entry.getKey(), supplier.apply(serviceLocator))
                    ));
            supervisorController.start();
            assignmentCache.start();
            maxRateSupervisor.start();
            serviceLocator.getService(ConsumersRuntimeMonitor.class).start();
            consumerHttpServer.start();
            httpClientsWorkloadReporter.start();
            hooksHandler.startup(serviceLocator);
        } catch (Exception e) {
            logger.error("Exception while starting Hermes Consumers", e);
        }
    }

    public void stop() {
        hooksHandler.shutdown(serviceLocator);
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
