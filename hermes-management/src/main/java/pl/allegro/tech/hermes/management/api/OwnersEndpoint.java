package pl.allegro.tech.hermes.management.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Owner;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSourceNotFound;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;

import javax.ws.rs.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/owners")
@Api(value = "/owners", description = "Provides owners information")
public class OwnersEndpoint {

    private final OwnerSources ownerSources;

    @Autowired
    public OwnersEndpoint(OwnerSources ownerSources) {
        this.ownerSources = ownerSources;
    }

    @GET
    @Path("/sources/{source}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists owners from the given source matching the search string", response = List.class, httpMethod = HttpMethod.GET)
    public List<Owner> search(@PathParam("source") String source,
                              @QueryParam("search") String searchString) {
        return ownerSources.getAutocompletionFor(source).ownersMatching(searchString);
    }

    @GET
    @Path("/sources/{source}/{id}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Returns owner from the given source of the given id", response = List.class, httpMethod = HttpMethod.GET)
    public Owner get(@PathParam("source") String source,
                     @PathParam("id") String id) {
        return ownerSources.getByName(source)
                .map(s -> s.get(id))
                .orElseThrow(() -> new OwnerSourceNotFound(source));
    }

    @GET
    @Path("/sources")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists owner sources", response = List.class, httpMethod = HttpMethod.GET)
    public List<SourceDescriptor> listSources() {
        return StreamSupport.stream(ownerSources.spliterator(), false)
                .map(SourceDescriptor::of)
                .collect(Collectors.toList());
    }

    private static class SourceDescriptor {

        @JsonProperty("name")
        private final String name;

        @JsonProperty("autocomplete")
        private final boolean autocomplete;

        @JsonProperty("deprecated")
        private final boolean deprecated;

        private SourceDescriptor(OwnerSource source) {
            this.name = source.name();
            this.autocomplete = source.autocompletion().isPresent();
            this.deprecated = source.isDeprecated();
        }

        static SourceDescriptor of(OwnerSource source) {
            return new SourceDescriptor(source);
        }

    }

}