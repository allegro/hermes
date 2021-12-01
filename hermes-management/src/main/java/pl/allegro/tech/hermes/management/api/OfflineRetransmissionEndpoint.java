package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionService;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("offline-retransmission/tasks")
@Api(value = "offline-retransmission/tasks", description = "Offline retransmission operations")
public class OfflineRetransmissionEndpoint {

    private final OfflineRetransmissionService retransmissionService;

    public OfflineRetransmissionEndpoint(OfflineRetransmissionService retransmissionService) {
        this.retransmissionService = retransmissionService;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public Response createRetransmissionTask(@Valid OfflineRetransmissionRequest request) {
        retransmissionService.createTask(request);
        return Response.status(Response.Status.CREATED).build();
    }


    @GET
    @Produces(APPLICATION_JSON)
    public List<OfflineRetransmissionTask> getAllRetransmissionTasks() {
        return retransmissionService.getAllTasks();
    }

    @DELETE
    @Path("/{taskId}")
    public Response createRetransmissionTask(@PathParam("taskId") String taskId) {
        retransmissionService.deleteTask(taskId);
        return Response.status(Response.Status.OK).build();
    }

}

