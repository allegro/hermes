package pl.allegro.tech.hermes.management.api;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Provider
public class CORSFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Credentials", "true");
        headers.add("Access-Control-Allow-Methods", "POST,PUT,GET,HEAD,DELETE");
        headers.add("Access-Control-Max-Age", "1209600");
        headers.addAll(
                "Access-Control-Allow-Headers",
                "X-Requested-With",
                "Content-Type",
                "Accept",
                "Origin",
                "Authorization"
        );
    }
}