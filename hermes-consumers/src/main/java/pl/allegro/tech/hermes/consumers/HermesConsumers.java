package pl.allegro.tech.hermes.consumers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsWorkloadReporter;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderProviders;
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

public class HermesConsumers implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(HermesConsumers.class);

    private final ConfigurableApplicationContext applicationContext;

    private final SpringHooksHandler springHooksHandler;
    private final ConsumerHttpServer consumerHttpServer;
    private final Trackers trackers;
    private final List<LogRepository> logRepositories;
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

    public void start() {
        try {
            oAuthHttpClient.start();
            logRepositories.forEach(trackers::add);
            messageSenderProviders.populateMessageSenderFactory(messageSenderFactory);

            consumerNodesRegistry.start();
            supervisorController.start();
            assignmentCache.start();
            maxRateSupervisor.start();
            consumersRuntimeMonitor.start();
            consumerHttpServer.start();
            httpClientsWorkloadReporter.start();
            springHooksHandler.startup(applicationContext);
        } catch (Exception e) {
            logger.error("Exception while starting Hermes Consumers", e);
        }
    }

    public void stop() {
        springHooksHandler.shutdown(applicationContext);
    }

    @Override
    public void run(String... args) throws Exception {
        this.start();
    }
}
