package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.Group;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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


