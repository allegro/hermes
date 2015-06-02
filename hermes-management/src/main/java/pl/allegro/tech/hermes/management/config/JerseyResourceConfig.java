package pl.allegro.tech.hermes.management.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;
import java.util.List;

@ApplicationPath("/")
public class JerseyResourceConfig extends ResourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(JerseyResourceConfig.class);
    
    public JerseyResourceConfig(List<String> packagesToScan) {
        packages(true, "pl.allegro.tech.hermes.management.api");
        
        for(String packageToScan : packagesToScan) {
            packages(true, packageToScan);
            logger.info("Scanning Jersey resources in: {}", packageToScan);
        }
        property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
        register(RolesAllowedDynamicFeature.class);
    }
    
}
