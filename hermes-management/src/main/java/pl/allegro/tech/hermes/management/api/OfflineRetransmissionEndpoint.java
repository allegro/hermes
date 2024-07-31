package pl.allegro.tech.hermes.management.api;

import io.swagger.annotations.Api;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.api.auth.ManagementRights;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionService;

import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("offline-retransmission/tasks")
@Api(value = "offline-retransmission/tasks", description = "Offline retransmission operations")
public class OfflineRetransmissionEndpoint {

    private final OfflineRetransmissionService retransmissionService;
    private final RetransmissionPermissions permissions;
    private final OfflineRetransmissionAuditor auditor;

    public OfflineRetransmissionEndpoint(OfflineRetransmissionService retransmissionService,
                                         TopicRepository topicRepository, ManagementRights managementRights) {
        this.retransmissionService = retransmissionService;
        this.permissions = new RetransmissionPermissions(topicRepository, managementRights);
        this.auditor = new OfflineRetransmissionAuditor();
    }

    @POST
    @Consumes(APPLICATION_JSON)
    public Response createRetransmissionTask(@Valid OfflineRetransmissionRequest request, @Context ContainerRequestContext requestContext) {
        System.out.println("Offline retransmission request: " + request);
        retransmissionService.validateRequest(request);
        permissions.ensurePermissionsToBothTopics(request, requestContext);
        var task = retransmissionService.createTask(request);
        auditor.auditRetransmissionCreation(request, requestContext, task);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    public List<OfflineRetransmissionTask> getAllRetransmissionTasks() {
        return retransmissionService.getAllTasks();
    }

    @DELETE
    @Path("/{taskId}")
    public Response deleteRetransmissionTask(@PathParam("taskId") String taskId) {
        retransmissionService.deleteTask(taskId);
        return Response.status(Response.Status.OK).build();
    }

    private static class RetransmissionPermissions {
        private final TopicRepository topicRepository;
        private final ManagementRights managementRights;


        private RetransmissionPermissions(TopicRepository topicRepository, ManagementRights managementRights) {
            this.topicRepository = topicRepository;
            this.managementRights = managementRights;
        }

        private void ensurePermissionsToBothTopics(OfflineRetransmissionRequest request, ContainerRequestContext requestContext) {
            var targetTopic = topicRepository.getTopicDetails(TopicName.fromQualifiedName(request.getTargetTopic()));
            var hasPermissions = validateSourceTopic(request.getSourceTopic(), requestContext)
                    && managementRights.isUserAllowedToManageTopic(targetTopic, requestContext);
            if (!hasPermissions) {
                throw new PermissionDeniedException("User needs permissions to source and target topics.");
            }
        }

        private boolean validateSourceTopic(Optional<String> sourceTopic, ContainerRequestContext requestContext) {
            return sourceTopic.isEmpty() || managementRights.isUserAllowedToManageTopic(
                    topicRepository.getTopicDetails(TopicName.fromQualifiedName(sourceTopic.get())),
                    requestContext
            );
        }
    }

    private static class OfflineRetransmissionAuditor {
        private static final Logger logger = LoggerFactory.getLogger(OfflineRetransmissionAuditor.class);

        public void auditRetransmissionCreation(OfflineRetransmissionRequest request, ContainerRequestContext requestContext, OfflineRetransmissionTask task) {
            String username = extractUsername(requestContext);
            logger.info("User {} created offline retransmission task: {}, taskId: {}", username, request, task.getTaskId());
        }

        private String extractUsername(ContainerRequestContext requestContext) {
            return requestContext.getSecurityContext().getUserPrincipal().getName();
        }
    }
}

