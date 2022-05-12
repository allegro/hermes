package pl.allegro.tech.hermes.frontend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.integration.env.IntegrationTestKafkaNamesMapperFactory;

@Configuration
public class KafkaNamesMapperConfiguration {

    @Bean
    @Primary
    @Profile("integration")
    public KafkaNamesMapper testKafkaNamesMapper(ConfigFactory configFactory) {
        return new IntegrationTestKafkaNamesMapperFactory(configFactory.getStringProperty(Configs.KAFKA_NAMESPACE)).create();
    }
}
