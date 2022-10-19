package pl.allegro.tech.hermes.management.api;

import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.management.config.CorsProperties;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
public class CorsFilter implements ContainerResponseFilter {

    private final CorsProperties corsProperties;

    @Autowired
    public CorsFilter(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
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
                "Hermes-Admin-Password"
        );
    }
}
