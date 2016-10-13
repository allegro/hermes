package pl.allegro.tech.hermes.management;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.common.filtering.MessageFilters;
import pl.allegro.tech.hermes.common.filtering.SubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.common.filtering.avro.AvroPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.common.filtering.json.JsonPathSubscriptionMessageFilterCompiler;
import pl.allegro.tech.hermes.management.config.SupportTeamServiceProperties;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties(SupportTeamServiceProperties.class)
public class HermesManagement {

    public static void main(String[] args) {
        SpringApplication.run(HermesManagement.class, args);
    }

    @Bean(name = "managementRequestFactory")
    @ConfigurationProperties(prefix = "management.restTemplate")
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory();
    }

    @Bean
    public RestTemplate restTemplate(@Qualifier("managementRequestFactory") ClientHttpRequestFactory clientHttpRequestFactory) {
        return new RestTemplate(clientHttpRequestFactory);
    }

    @Bean
    public MessageFilters messageFilters() {
        List<SubscriptionMessageFilterCompiler> availableFilters = new ArrayList<>();
        availableFilters.add(new JsonPathSubscriptionMessageFilterCompiler());
        availableFilters.add(new AvroPathSubscriptionMessageFilterCompiler());
        return new MessageFilters(new ArrayList<>(), availableFilters);
    }
}
