package pl.allegro.tech.hermes.consumers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsWorkloadReporter;
import pl.allegro.tech.hermes.consumers.di.config.MessageSenderProviders;
import pl.allegro.tech.hermes.consumers.health.ConsumerHttpServer;
import pl.allegro.tech.hermes.consumers.hooks.SpringFlushLogsShutdownHook;
import pl.allegro.tech.hermes.consumers.hooks.SpringHooksHandler;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.List;

import static pl.allegro.tech.hermes.common.config.Configs.CONSUMER_SIGNAL_PROCESSING_QUEUE_SIZE;

public class HermesConsumers implements CommandLineRunner {//TODO: use as bean?

    private static final Logger logger = LoggerFactory.getLogger(HermesConsumers.class);

    private final ConfigurableApplicationContext applicationContext;

    private final SpringHooksHandler springHooksHandler;
    private final ConsumerHttpServer consumerHttpServer;
    private final Trackers trackers;
    private final List<LogRepository> logRepositories;
//    private Map<String, LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>>> springMessageSenderProvidersSuppliers;
//    private Map<String, List<ProtocolMessageSenderProvider>> springMessageSenderProviders;
    private final MessageSenderProviders messageSenderProviders;
    private final MessageSenderFactory messageSenderFactory;
    private final ConsumerNodesRegistry consumerNodesRegistry;
    private final SupervisorController supervisorController;
    private final MaxRateSupervisor maxRateSupervisor;
    private final ConsumerAssignmentCache assignmentCache;
    private final OAuthClient oAuthHttpClient;
    private final HttpClientsWorkloadReporter httpClientsWorkloadReporter;
    private final ConsumersRuntimeMonitor consumersRuntimeMonitor;

    public HermesConsumers(SpringHooksHandler springHooksHandler,
                           ConsumerHttpServer consumerHttpServer,
                           Trackers trackers,
                           List<LogRepository> logRepositories,
                           MessageSenderProviders messageSenderProviders,
                           MessageSenderFactory messageSenderFactory,
                           ConsumerNodesRegistry consumerNodesRegistry,
                           SupervisorController supervisorController,
                           MaxRateSupervisor maxRateSupervisor,
                           ConsumerAssignmentCache assignmentCache,
                           OAuthClient oAuthHttpClient,
                           HttpClientsWorkloadReporter httpClientsWorkloadReporter,
                           ConsumersRuntimeMonitor consumersRuntimeMonitor,
                           ConfigurableApplicationContext applicationContext) {
        this.springHooksHandler = springHooksHandler;
        this.consumerHttpServer = consumerHttpServer;
        this.trackers = trackers;
        this.logRepositories = logRepositories;
        this.messageSenderProviders = messageSenderProviders;
        this.messageSenderFactory = messageSenderFactory;
        this.consumerNodesRegistry = consumerNodesRegistry;
        this.supervisorController = supervisorController;
        this.maxRateSupervisor = maxRateSupervisor;
        this.assignmentCache = assignmentCache;
        this.oAuthHttpClient = oAuthHttpClient;
        this.httpClientsWorkloadReporter = httpClientsWorkloadReporter;
        this.consumersRuntimeMonitor = consumersRuntimeMonitor;
        boolean flushLogsShutdownHookEnabled = true;
        this.applicationContext = applicationContext;


        this.springHooksHandler.addShutdownHook((s) -> {
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

//    HermesConsumers(SpringHooksHandler springHooksHandler,
//                    List<ImmutablePair<Class<?>, Supplier<?>>> builderBeans,
//                    Map<String, LinkedList<Function<ApplicationContext, ProtocolMessageSenderProvider>>> springMessageSenderProvidersSuppliers,
//                    List<Function<ApplicationContext, LogRepository>> springLogRepositories,
//                    boolean flushLogsShutdownHookEnabled) {
//
//        this.springHooksHandler = springHooksHandler;
//        this.springMessageSenderProvidersSuppliers = springMessageSenderProvidersSuppliers;
//        this.springLogRepositories = springLogRepositories;
//
////        serviceLocator = createDIContainer(binders);//inject all config binders' classes into IoC container
////        SpringBridge.getSpringBridge().initializeSpringBridge(serviceLocator);
//        registerAllBeansAsPrimary(builderBeans);
//
//        //get all "beans" from IoC container
////        trackers = serviceLocator.getService(Trackers.class);
//        trackers = applicationContext.getBean(Trackers.class);
////        consumerHttpServer = serviceLocator.getService(ConsumerHttpServer.class);
//        consumerHttpServer = applicationContext.getBean(ConsumerHttpServer.class);
////        messageSenderFactory = serviceLocator.getService(MessageSenderFactory.class);
//        messageSenderFactory2 = applicationContext.getBean(MessageSenderFactory.class);
//
////        consumerNodesRegistry = serviceLocator.getService(ConsumerNodesRegistry.class);
//        consumerNodesRegistry = applicationContext.getBean(ConsumerNodesRegistry.class);
////        supervisorController = serviceLocator.getService(SupervisorController.class);
//        supervisorController = applicationContext.getBean(SupervisorController.class);
////        maxRateSupervisor = serviceLocator.getService(MaxRateSupervisor.class);
//        maxRateSupervisor = applicationContext.getBean(MaxRateSupervisor.class);
////        assignmentCache = serviceLocator.getService(ConsumerAssignmentCache.class);
//        assignmentCache = applicationContext.getBean("consumerAssignmentCache", ConsumerAssignmentCache.class);
////        oAuthHttpClient = serviceLocator.getService(OAuthClient.class);
//        oAuthHttpClient = applicationContext.getBean(OAuthClient.class);
////        httpClientsWorkloadReporter = serviceLocator.getService(HttpClientsWorkloadReporter.class);
//        httpClientsWorkloadReporter = applicationContext.getBean(HttpClientsWorkloadReporter.class);
//
////        hooksHandler.addShutdownHook((s) -> { //TODO: do we need hooks for Spring?
////            try {
////                consumerHttpServer.stop();
////                maxRateSupervisor.stop();
////                assignmentCache.stop();
////                oAuthHttpClient.stop();
////                consumerNodesRegistry.stop();
////                supervisorController.shutdown();
////                s.shutdown();
////            } catch (Exception e) {
////                logger.error("Exception while shutdown Hermes Consumers", e);
////            }
////        });
////        if (flushLogsShutdownHookEnabled) {
////            hooksHandler.addShutdownHook(new FlushLogsShutdownHook());
////        }
//
//        springHooksHandler.addShutdownHook((s) -> {
//            try {
//                consumerHttpServer.stop();
//                maxRateSupervisor.stop();
//                assignmentCache.stop();
//                oAuthHttpClient.stop();
//                consumerNodesRegistry.stop();
//                supervisorController.shutdown();
//                applicationContext.registerShutdownHook();
//            } catch (Exception e) {
//                logger.error("Exception while shutdown Hermes Consumers", e);
//            }
//        });
//        if (flushLogsShutdownHookEnabled) {
//            springHooksHandler.addShutdownHook(new SpringFlushLogsShutdownHook());
//        }
//    }

    public void start() {
        try {
            oAuthHttpClient.start();

            logRepositories.forEach(trackers::add);

            messageSenderProviders.populateMessageSenderFactory(messageSenderFactory);

//            springMessageSenderProviders.entrySet().forEach(entry ->
//                    entry.getValue().forEach(messageSender ->
//                            messageSenderFactory.addSupportedProtocol(entry.getKey(), messageSender)
//                    ));
            consumerNodesRegistry.start();
            supervisorController.start();
            assignmentCache.start();
            maxRateSupervisor.start();
            consumersRuntimeMonitor.start();
//            applicationContext.getBean(ConsumersRuntimeMonitor.class).start();
            consumerHttpServer.start();
            httpClientsWorkloadReporter.start();
            springHooksHandler.startup(applicationContext);
        } catch (Exception e) {
            logger.error("Exception while starting Hermes Consumers", e);
        }
    }

    public void stop() {
//        hooksHandler.shutdown(serviceLocator);
        springHooksHandler.shutdown(applicationContext);//TODO
    }

    @Override
    public void run(String... args) throws Exception {
        this.start();
    }
}
