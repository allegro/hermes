package pl.allegro.tech.hermes.management.config;

import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.common.json.MessageContentWrapper;

@Configuration
@EnableConfigurationProperties(MessageProperties.class)
public class MessageConfiguration {

    @Autowired
    MessageProperties messageProperties;

    @Bean
    ObjectMapper fastObjectMapper() {
        return JsonFactory.create();
    }

    @Bean
    MessageContentWrapper messageContentWrapper() {
        return new MessageContentWrapper(messageProperties.getContentRoot(),
                messageProperties.getMetadataContentRoot(),
                fastObjectMapper());
    }
}
