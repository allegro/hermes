package pl.allegro.tech.hermes.management.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.management.config.CorsProperties;

@Provider
public class CorsFilter implements ContainerResponseFilter {

  private final CorsProperties corsProperties;

  @Autowired
  public CorsFilter(CorsProperties corsProperties) {
    this.corsProperties = corsProperties;
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MultivaluedMap<String, Object> headers = responseContext.getHeaders();
    headers.add("Access-Control-Allow-Origin", corsProperties.getAllowedOrigin());
    headers.add("Access-Control-Allow-Methods", "POST,PUT,GET,HEAD,DELETE");
    headers.add("Access-Control-Max-Age", "1209600");
    headers.addAll(
        "Access-Control-Allow-Headers",
        "X-Requested-With",
        "Content-Type",
        "Accept",
        "Origin",
        "Authorization",
        "Hermes-Admin-Password");
  }
}
