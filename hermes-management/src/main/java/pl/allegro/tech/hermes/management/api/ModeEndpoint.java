package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.management.domain.mode.ModeService.ManagementMode.READ_ONLY;
import static pl.allegro.tech.hermes.management.domain.mode.ModeService.ManagementMode.READ_WRITE;

@Component
@Path("/mode")
@Api(value = "/mode", description = "Operations on management mode")
public class ModeEndpoint {

    private final ModeService modeService;

    public ModeEndpoint(ModeService modeService) {
        this.modeService = modeService;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get management mode", response = String.class, httpMethod = HttpMethod.GET)
    public String getMode() {
        return modeService.getMode().toString();
    }

    @POST
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Set management mode", response = String.class, httpMethod = HttpMethod.POST)
    @RolesAllowed(Roles.ADMIN)
    public Response setMode(@QueryParam("mode") String mode) {
        if (mode == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        switch (mode) {
            case ModeService.READ_WRITE:
                modeService.setMode(READ_WRITE);
                break;
            case ModeService.READ_ONLY:
                modeService.setMode(READ_ONLY);
                break;
            default:
                return Response.status(Response.Status.BAD_REQUEST).build();
        }
        return Response.status(Response.Status.OK).build();
    }
}
