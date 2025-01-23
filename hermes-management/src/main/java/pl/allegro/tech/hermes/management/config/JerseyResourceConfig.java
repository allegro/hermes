package pl.allegro.tech.hermes.management.config;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.glassfish.jersey.servlet.ServletProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationPath("/")
public class JerseyResourceConfig extends ResourceConfig {

  private static final Logger logger = LoggerFactory.getLogger(JerseyResourceConfig.class);

  public JerseyResourceConfig(JerseyProperties jerseyProperties) {
    packages(true, "pl.allegro.tech.hermes.management.api");

    for (String packageToScan : jerseyProperties.getPackagesToScan()) {
      packages(true, packageToScan);
      logger.info("Scanning Jersey resources in: {}", packageToScan);
    }
    property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    property(
        ServletProperties.FILTER_STATIC_CONTENT_REGEX,
        jerseyProperties.getFilterStaticContentRegexp());
    register(RolesAllowedDynamicFeature.class);
    property(FreemarkerMvcFeature.TEMPLATE_BASE_PATH, "static");
    register(FreemarkerMvcFeature.class);
  }
}
