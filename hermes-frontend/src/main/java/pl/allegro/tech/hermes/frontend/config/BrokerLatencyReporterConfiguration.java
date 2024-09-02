package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.common.metric.executor.InstrumentedExecutorServiceFactory;
import pl.allegro.tech.hermes.frontend.producer.BrokerLatencyReporter;

import java.util.concurrent.ExecutorService;

@Configuration
@EnableConfigurationProperties(BrokerLatencyReporterProperties.class)
public class BrokerLatencyReporterConfiguration {

    @Bean
    BrokerLatencyReporter brokerLatencyReporter(
            BrokerLatencyReporterProperties properties,
            MetricsFacade metricsFacade,
            InstrumentedExecutorServiceFactory executorServiceFactory) {
        ExecutorService executorService =
                executorServiceFactory.getExecutorService(
                        "broker-latency-reporter",
                        properties.getThreadPoolSize(),
                        true,
                        properties.getThreadPoolQueueCapacity());

        return new BrokerLatencyReporter(
                properties.isEnabled(),
                metricsFacade,
                properties.getSlowResponseLoggingThreshold(),
                executorService);
    }
}
