package pl.allegro.tech.hermes.management.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Maintainer;
import pl.allegro.tech.hermes.common.exception.HermesException;
import pl.allegro.tech.hermes.management.domain.maintainer.HintingMaintainerSource;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSource;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSourceNotFound;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSources;

import javax.ws.rs.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/maintainers")
@Api(value = "/maintainers", description = "Provides maintainers information")
public class MaintainersEndpoint {

    private MaintainerSources maintainerSources;

    @Autowired
    public MaintainersEndpoint(MaintainerSources maintainerSources) {
        this.maintainerSources = maintainerSources;
    }

    @GET
    @Path("/sources/{source}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists maintainers from the given source matching the search string", response = List.class, httpMethod = HttpMethod.GET)
    public List<Maintainer> search(@PathParam("source") String source,
                                   @QueryParam("search") String searchString) {
        MaintainerSource maintainerSource = maintainerSources.getByName(source).orElseThrow(() -> new MaintainerSourceNotFound(source));
        if (!(maintainerSource instanceof HintingMaintainerSource)) {
            throw new HintingNotSupportedException(maintainerSource);
        }
        return ((HintingMaintainerSource) maintainerSource).maintainersMatching(searchString);
    }

    @GET
    @Path("/sources/{source}/{id}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Returns maintainer from the given source of the given id", response = List.class, httpMethod = HttpMethod.GET)
    public Maintainer get(@PathParam("source") String source,
                          @PathParam("id") String id) {
        return maintainerSources.getByName(source)
                .map(s -> s.get(id))
                .orElseThrow(() -> new MaintainerSourceNotFound(source));
    }

    @GET
    @Path("/sources")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists maintainer sources", response = List.class, httpMethod = HttpMethod.GET)
    public List<SourceDescriptor> listSources() {
        return StreamSupport.stream(maintainerSources.spliterator(), false)
                .map(SourceDescriptor::of)
                .collect(Collectors.toList());
    }

    private static class SourceDescriptor {

        @JsonProperty("name")
        private final String name;

        @JsonProperty("hinting")
        private final boolean hinting;

        private SourceDescriptor(String name, boolean hinting) {
            this.name = name;
            this.hinting = hinting;
        }

        static SourceDescriptor of(MaintainerSource source) {
            return new SourceDescriptor(source.name(), source instanceof HintingMaintainerSource);
        }

    }

    private static class HintingNotSupportedException extends HermesException {

        HintingNotSupportedException(MaintainerSource source) {
            super("Maintainer source '" + source.name() + "' doesn't support hinting.");
        }

        @Override
        public ErrorCode getCode() {
            return ErrorCode.MAINTAINER_SOURCE_DOESNT_SUPPORT_HINTING;
        }

    }

}