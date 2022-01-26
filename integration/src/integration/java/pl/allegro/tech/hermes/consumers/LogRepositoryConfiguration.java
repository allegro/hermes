package pl.allegro.tech.hermes.consumers;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.DB;
import org.springframework.context.annotation.Bean;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.env.FongoFactory;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.tracker.consumers.LogRepository;
import pl.allegro.tech.hermes.tracker.mongo.consumers.MongoLogRepository;

public class LogRepositoryConfiguration {

    @Bean
    LogRepository logRepository(ConfigFactory configFactory, MetricRegistry metricRegistry, PathsCompiler pathsCompiler) {

        return new MongoLogRepository(FongoFactory.hermesDB(),
                10,
                1000,
                configFactory.getStringProperty(Configs.KAFKA_CLUSTER_NAME),
                configFactory.getStringProperty(Configs.HOSTNAME),
                metricRegistry,
                pathsCompiler);
    }
}
