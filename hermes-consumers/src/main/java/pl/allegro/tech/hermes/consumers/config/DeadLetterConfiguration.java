package pl.allegro.tech.hermes.consumers.config;


import com.google.api.Metric;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.ToProtoConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.dead.*;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadLetters;
import pl.allegro.tech.hermes.tracker.consumers.deadletters.DeadRepository;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@Configuration

public class DeadLetterConfiguration {

    @Bean(destroyMethod = "close")
    public DeadLetters deadLetters(List<DeadRepository> repositories){
        return new DeadLetters(repositories);
    }
    @Bean
    public DeadMessageToProtoConverter deadMessageToProtoConverter() {
        return new DeadMessageToProtoConverter();
    }

}
