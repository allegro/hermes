package pl.allegro.tech.hermes.frontend;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.frontend.config.KafkaProperties;
import pl.allegro.tech.hermes.integration.env.IntegrationTestKafkaNamesMapperFactory;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaNamesMapperConfiguration {

    @Bean
    @Primary
    @Profile("integration")
    public KafkaNamesMapper testKafkaNamesMapper(KafkaProperties kafkaProperties) {
        return new IntegrationTestKafkaNamesMapperFactory(kafkaProperties.getNamespace()).create();
    }
}
