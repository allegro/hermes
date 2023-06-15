package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import pl.allegro.tech.hermes.api.Group;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("groups")
public interface GroupEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    List<String> list();

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{groupName}")
    Group get(@PathParam("groupName") String groupName);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Response create(Group group);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{groupName}")
    Response update(@PathParam("groupName") String groupName, Group group);

    @DELETE
    @Path("/{groupName}")
    Response delete(@PathParam("groupName") String groupName);

}


