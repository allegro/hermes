package pl.allegro.tech.hermes.management.api;

import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.config.storage.StorageClustersProperties;
import pl.allegro.tech.hermes.management.config.storage.StorageProperties;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("datacenters")
@Component
public class DatacenterEndpoint {

    private final List<String> datacenters;

    DatacenterEndpoint(StorageClustersProperties clustersProperties) {
        datacenters = clustersProperties.getClusters()
                .stream()
                .map(StorageProperties::getDatacenter)
                .collect(Collectors.toList());
    }

    @GET
    @Produces(APPLICATION_JSON)
    public List<String> getDatacenters() {
        return datacenters;
    }
}