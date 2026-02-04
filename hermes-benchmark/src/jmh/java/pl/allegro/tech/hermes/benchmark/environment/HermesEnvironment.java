package pl.allegro.tech.hermes.benchmark.environment;

import static pl.allegro.tech.hermes.consumers.ConsumersConfigurationProperties.CONSUMER_MAX_RATE_BALANCE_INTERVAL;
import static pl.allegro.tech.hermes.consumers.ConsumersConfigurationProperties.CONSUMER_MAX_RATE_UPDATE_INTERVAL;
import static pl.allegro.tech.hermes.consumers.ConsumersConfigurationProperties.CONSUMER_RATE_CONVERGENCE_FACTOR;
import static pl.allegro.tech.hermes.consumers.ConsumersConfigurationProperties.CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY;
import static pl.allegro.tech.hermes.consumers.ConsumersConfigurationProperties.CONSUMER_RATE_LIMITER_SUPERVISOR_PERIOD;
import static pl.allegro.tech.hermes.test.helper.frontend.FrontendConfigurationProperties.*;
import static pl.allegro.tech.hermes.test.helper.frontend.FrontendConfigurationProperties.FRONTEND_THROUGHPUT_TYPE;

import java.time.Duration;
import java.util.stream.Stream;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.consumers.HermesConsumersTestApp;
import pl.allegro.tech.hermes.management.HermesManagementTestApp;
import pl.allegro.tech.hermes.test.helper.client.integration.HermesInitHelper;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.environment.HermesTestApp;
import pl.allegro.tech.hermes.test.helper.frontend.HermesFrontendTestApp;

@State(Scope.Benchmark)
public class HermesEnvironment {

  private static final ZookeeperContainer hermesZookeeper =
      new ZookeeperContainer("HermesZookeeper");
  private static final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
  public static final ConfluentSchemaRegistryContainer schemaRegistry =
      new ConfluentSchemaRegistryContainer().withKafkaCluster(kafka);
  private static final HermesConsumersTestApp consumers =
      new HermesConsumersTestApp(hermesZookeeper, kafka, schemaRegistry);
  private static final HermesManagementTestApp management =
      new HermesManagementTestApp(hermesZookeeper, kafka, schemaRegistry);
  private static final HermesFrontendTestApp frontend =
      new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry);
  HermesInitHelper hermesHelper;

  private static boolean started = false;

  @Setup(Level.Trial)
  public void setupEnvironment() {
    if (!started) {
      Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
      schemaRegistry.start();
      management.start();
      configureFrontend();
      configureConsumer();
      Stream.of(consumers, frontend).forEach(HermesTestApp::start);
      started = true;
    }
    Stream.of(management, consumers, frontend)
        .forEach(
            app -> {
              if (app.shouldBeRestarted()) {
                app.stop();
                app.start();
              }
            });
    hermesHelper = new HermesInitHelper(management.getPort());
  }

  private static void configureConsumer() {
    consumers.withProperty(CONSUMER_MAX_RATE_UPDATE_INTERVAL, Duration.ofSeconds(1));
    consumers.withProperty(CONSUMER_MAX_RATE_BALANCE_INTERVAL, Duration.ofSeconds(1));
    consumers.withProperty(CONSUMER_RATE_LIMITER_SUPERVISOR_PERIOD, Duration.ofSeconds(1));
    consumers.withProperty(CONSUMER_RATE_LIMITER_HEARTBEAT_MODE_DELAY, Duration.ofSeconds(1));
    consumers.withProperty(CONSUMER_RATE_CONVERGENCE_FACTOR, 1);
  }

  private static void configureFrontend() {
    frontend.withProperty(FRONTEND_THROUGHPUT_TYPE, "unlimited");
    frontend.withProperty(MESSAGES_LOCAL_STORAGE_ENABLED, false);
  }

  @TearDown(Level.Trial)
  public void shutdownServers() {
    Stream.of(management, consumers, frontend).parallel().forEach(HermesTestApp::stop);
    Stream.of(hermesZookeeper, kafka, schemaRegistry).parallel().forEach(Startable::stop);
    started = false;
  }

  public int frontendPort() {
    return frontend.getPort();
  }
}
