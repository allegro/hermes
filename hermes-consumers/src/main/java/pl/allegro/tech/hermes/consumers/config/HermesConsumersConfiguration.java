package pl.allegro.tech.hermes.consumers.config;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.consumers.consumer.oauth.client.OAuthClient;
import pl.allegro.tech.hermes.consumers.consumer.rate.maxrate.MaxRateSupervisor;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderFactory;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderProviders;
import pl.allegro.tech.hermes.consumers.consumer.sender.http.HttpClientsWorkloadReporter;
import pl.allegro.tech.hermes.consumers.health.ConsumerHttpServer;
import pl.allegro.tech.hermes.consumers.hooks.SpringHooksHandler;
import pl.allegro.tech.hermes.consumers.registry.ConsumerNodesRegistry;
import pl.allegro.tech.hermes.consumers.supervisor.monitor.ConsumersRuntimeMonitor;
import pl.allegro.tech.hermes.consumers.supervisor.workload.ConsumerAssignmentCache;
import pl.allegro.tech.hermes.consumers.supervisor.workload.SupervisorController;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.List;

@Configuration
public class HermesConsumersConfiguration {

    @Bean
    public HermesConsumers hermesConsumers(SpringHooksHandler springHooksHandler,
                                           ConsumerHttpServer consumerHttpServer,
                                           Trackers trackers,
                                           List<LogRepository> logRepositories,
                                           MessageSenderProviders messageSenderProviders,
                                           MessageSenderFactory messageSenderFactor,
                                           ConsumerNodesRegistry consumerNodesRegistry,
                                           SupervisorController supervisorController,
                                           MaxRateSupervisor maxRateSupervisor,
                                           ConsumerAssignmentCache assignmentCache,
                                           OAuthClient oAuthHttpClient,
                                           HttpClientsWorkloadReporter httpClientsWorkloadReporter,
                                           ConsumersRuntimeMonitor consumersRuntimeMonitor,
                                           ConfigurableApplicationContext applicationContext) {
        return new HermesConsumers(springHooksHandler, consumerHttpServer, trackers, logRepositories,
                messageSenderProviders, messageSenderFactor, consumerNodesRegistry, supervisorController,
                maxRateSupervisor, assignmentCache, oAuthHttpClient, httpClientsWorkloadReporter, consumersRuntimeMonitor,
                applicationContext);
    }

}
