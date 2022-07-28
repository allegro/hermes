package pl.allegro.tech.hermes.frontend;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
//todo change to frontend module
import pl.allegro.tech.hermes.consumers.config.KafkaClustersProperties;
import pl.allegro.tech.hermes.integration.env.IntegrationTestKafkaNamesMapperFactory;

@Configuration
@EnableConfigurationProperties(KafkaClustersProperties.class)
public class KafkaNamesMapperConfiguration {

    @Bean
    @Primary
    @Profile("integration")
    public KafkaNamesMapper testKafkaNamesMapper(KafkaClustersProperties kafkaClustersProperties) {
        return new IntegrationTestKafkaNamesMapperFactory(kafkaClustersProperties.getNamespace()).create();
    }
}
