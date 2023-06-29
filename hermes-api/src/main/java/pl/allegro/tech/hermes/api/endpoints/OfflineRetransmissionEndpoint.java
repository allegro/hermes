package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

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
