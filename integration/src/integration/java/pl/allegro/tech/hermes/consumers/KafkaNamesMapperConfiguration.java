package pl.allegro.tech.hermes.consumers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.integration.env.IntegrationTestKafkaNamesMapperFactory;

@Configuration
public class KafkaNamesMapperConfiguration {

    @Bean
    @Primary
    public KafkaNamesMapper kafkaNamesMapper(ConfigFactory configFactory) {
        return new IntegrationTestKafkaNamesMapperFactory(configFactory.getStringProperty(Configs.KAFKA_NAMESPACE)).create();
    }
}
