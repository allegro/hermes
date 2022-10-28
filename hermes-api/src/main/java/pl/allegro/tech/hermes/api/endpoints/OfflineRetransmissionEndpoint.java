package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("offline-retransmission/tasks")
public interface OfflineRetransmissionEndpoint {
    @POST
    @Consumes(APPLICATION_JSON)
    Response createRetransmissionTask(OfflineRetransmissionRequest request);

    @GET
    @Produces(APPLICATION_JSON)
    List<OfflineRetransmissionTask> getAllTasks();

    @DELETE
    @Path("/{taskId}")
    Response deleteRetransmissionTask(@PathParam("taskId") String taskId);
}
