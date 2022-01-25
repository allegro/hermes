package pl.allegro.tech.hermes.consumers;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.glassfish.hk2.utilities.Binder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.ProtocolMessageSenderProvider;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsWorkloadReporter;
import pl.allegro.tech.hermes.consumers.di.config.PrimaryBeanCustomizer;
import pl.allegro.tech.hermes.consumers.health.ConsumerHttpServer;
import pl.allegro.tech.hermes.consumers.hooks.SpringFlushLogsShutdownHook;
import pl.allegro.tech.hermes.consumers.hooks.SpringHooksHandler;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class HermesConsumers {//TODO: change to bean?

    private static final Logger logger = LoggerFactory.getLogger(HermesConsumers.class);

    private final SpringHooksHandler springHooksHandler;
    private final ConsumerHttpServer consumerHttpServer;
    private final Trackers trackers;
    private final List<Function<ApplicationContext, LogRepository>> springLogRepositories;
    private final Map<String, LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>>> springMessageSenderProvidersSuppliers;
    private final MessageSenderFactory messageSenderFactory2;

    private final ConsumerNodesRegistry consumerNodesRegistry;
    private final SupervisorController supervisorController;
    private final MaxRateSupervisor maxRateSupervisor;
    private final ConsumerAssignmentCache assignmentCache;
    private final OAuthClient oAuthHttpClient;
    private final HttpClientsWorkloadReporter httpClientsWorkloadReporter;

    private static GenericApplicationContext applicationContext;//TODO: static?

    public static void main(GenericApplicationContext applicationContext) {//TODO: change to singleton to avoid static?
        HermesConsumers.applicationContext = applicationContext;
        consumers().build().start();
    }

    HermesConsumers(SpringHooksHandler springHooksHandler,
//            HooksHandler hooksHandler,
                    List<Binder> binders,
                    List<ImmutablePair<Class<?>, Supplier<?>>> builderBeans,
//                    MultiMap<String, Function<ServiceLocator, ProtocolMessageSenderProvider>> messageSenderProvidersSuppliers,
                    Map<String, LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>>> springMessageSenderProvidersSuppliers,
//                    List<Function<ServiceLocator, LogRepository>> logRepositories,
                    List<Function<ApplicationContext, LogRepository>> springLogRepositories,
                    boolean flushLogsShutdownHookEnabled) {

        this.springHooksHandler = springHooksHandler;
        this.springMessageSenderProvidersSuppliers = springMessageSenderProvidersSuppliers;
        this.springLogRepositories = springLogRepositories;

//        serviceLocator = createDIContainer(binders);//inject all config binders' classes into IoC container
//        SpringBridge.getSpringBridge().initializeSpringBridge(serviceLocator);
        registerAllBeansAsPrimary(builderBeans);

        //get all "beans" from IoC container
//        trackers = serviceLocator.getService(Trackers.class);
        trackers = applicationContext.getBean(Trackers.class);
//        consumerHttpServer = serviceLocator.getService(ConsumerHttpServer.class);
        consumerHttpServer = applicationContext.getBean(ConsumerHttpServer.class);
//        messageSenderFactory = serviceLocator.getService(MessageSenderFactory.class);
        messageSenderFactory2 = applicationContext.getBean(MessageSenderFactory.class);

//        consumerNodesRegistry = serviceLocator.getService(ConsumerNodesRegistry.class);
        consumerNodesRegistry = applicationContext.getBean(ConsumerNodesRegistry.class);
//        supervisorController = serviceLocator.getService(SupervisorController.class);
        supervisorController = applicationContext.getBean(SupervisorController.class);
//        maxRateSupervisor = serviceLocator.getService(MaxRateSupervisor.class);
        maxRateSupervisor = applicationContext.getBean(MaxRateSupervisor.class);
//        assignmentCache = serviceLocator.getService(ConsumerAssignmentCache.class);
        assignmentCache = applicationContext.getBean("consumerAssignmentCache", ConsumerAssignmentCache.class);
//        oAuthHttpClient = serviceLocator.getService(OAuthClient.class);
        oAuthHttpClient = applicationContext.getBean(OAuthClient.class);
//        httpClientsWorkloadReporter = serviceLocator.getService(HttpClientsWorkloadReporter.class);
        httpClientsWorkloadReporter = applicationContext.getBean(HttpClientsWorkloadReporter.class);

//        hooksHandler.addShutdownHook((s) -> { //TODO: do we need hooks for Spring?
//            try {
//                consumerHttpServer.stop();
//                maxRateSupervisor.stop();
//                assignmentCache.stop();
//                oAuthHttpClient.stop();
//                consumerNodesRegistry.stop();
//                supervisorController.shutdown();
//                s.shutdown();
//            } catch (Exception e) {
//                logger.error("Exception while shutdown Hermes Consumers", e);
//            }
//        });
//        if (flushLogsShutdownHookEnabled) {
//            hooksHandler.addShutdownHook(new FlushLogsShutdownHook());
//        }

        springHooksHandler.addShutdownHook((s) -> {
            try {
                consumerHttpServer.stop();
                maxRateSupervisor.stop();
                assignmentCache.stop();
                oAuthHttpClient.stop();
                consumerNodesRegistry.stop();
                supervisorController.shutdown();
                applicationContext.registerShutdownHook();
            } catch (Exception e) {
                logger.error("Exception while shutdown Hermes Consumers", e);
            }
        });
        if (flushLogsShutdownHookEnabled) {
            springHooksHandler.addShutdownHook(new SpringFlushLogsShutdownHook());
        }
    }

    public void start() {
        try {
            oAuthHttpClient.start();
//            logRepositories.forEach(serviceLocatorLogRepositoryFunction ->
//                    trackers.add(serviceLocatorLogRepositoryFunction.apply(serviceLocator)));

            springLogRepositories.forEach(applicationContextLogRepositoryFunction ->
                    trackers.add(applicationContextLogRepositoryFunction.apply(applicationContext)));

//            messageSenderProvidersSuppliers.entrySet().stream().forEach(entry ->
//                    entry.getValue().stream().forEach(supplier ->
//                            messageSenderFactory.addSupportedProtocol(entry.getKey(), supplier.apply(serviceLocator))
//                    ));

            springMessageSenderProvidersSuppliers.entrySet().forEach(entry ->
                    entry.getValue().forEach(supplier ->
                            messageSenderFactory2.addSupportedProtocol(entry.getKey(), supplier.apply(applicationContext))
                    ));
            consumerNodesRegistry.start();
            supervisorController.start();
            assignmentCache.start();
            maxRateSupervisor.start();
//            serviceLocator.getService(ConsumersRuntimeMonitor.class).start();
            applicationContext.getBean(ConsumersRuntimeMonitor.class).start();
            consumerHttpServer.start();
            httpClientsWorkloadReporter.start();
//            hooksHandler.startup(serviceLocator);
            springHooksHandler.startup(applicationContext);
        } catch (Exception e) {
            logger.error("Exception while starting Hermes Consumers", e);
        }
    }

    public void stop() {
//        hooksHandler.shutdown(serviceLocator);
        springHooksHandler.shutdown(applicationContext);//TODO
    }

//    private ServiceLocator createDIContainer(List<Binder> binders) {
//        String uniqueName = "HermesConsumersLocator" + UUID.randomUUID();
//        return ServiceLocatorUtilities.bind(uniqueName, binders.toArray(new Binder[binders.size()]));
//    }

//    public <T> T getService(Class<T> clazz) {
//        return serviceLocator.getService(clazz);
//    }

//    public <T> T getService(Class<T> clazz, String name) {
//        return serviceLocator.getService(clazz, name);
//    }

    public static HermesConsumersBuilder consumers() {
        return new HermesConsumersBuilder();
    }

    private void registerAllBeansAsPrimary(List<ImmutablePair<Class<?>, Supplier<?>>> beans) {
        beans.forEach(this::registerPrimaryBean);
    }

    private void registerPrimaryBean(ImmutablePair<Class<?>, Supplier<?>> bean) {
        BeanDefinitionCustomizer primaryBeanCustomizer = new PrimaryBeanCustomizer();
        applicationContext.registerBean(bean.left, bean.right, primaryBeanCustomizer);
    }

}
