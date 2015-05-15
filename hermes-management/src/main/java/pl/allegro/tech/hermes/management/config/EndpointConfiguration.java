package pl.allegro.tech.hermes.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.allegro.tech.hermes.management.api.auth.AllowAllSecurityContextProvider;
import pl.allegro.tech.hermes.management.api.auth.AuthorizationFilter;
import pl.allegro.tech.hermes.management.api.auth.SecurityContextProvider;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("/")
public class EndpointConfiguration extends ResourceConfig {

    @Autowired
    ObjectMapper objectMapper;
    
    public EndpointConfiguration() {
        packages(true, "pl.allegro.tech.hermes.management.api");
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        register(ObjectMapperContextResolver.class);
        register(AuthorizationFilter.class);
        register(RolesAllowedDynamicFeature.class);
    }

    @Bean
    @ConditionalOnMissingBean(SecurityContextProvider.class)
    SecurityContextProvider authorization() {
        return new AllowAllSecurityContextProvider();
    }
}
