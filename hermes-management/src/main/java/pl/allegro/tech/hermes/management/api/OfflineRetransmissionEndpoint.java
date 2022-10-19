package pl.allegro.tech.hermes.management.api;

import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest;
import pl.allegro.tech.hermes.api.OfflineRetransmissionTask;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.topic.TopicRepository;
import pl.allegro.tech.hermes.management.api.auth.ManagementRights;
import pl.allegro.tech.hermes.management.domain.PermissionDeniedException;
import pl.allegro.tech.hermes.management.domain.retransmit.OfflineRetransmissionService;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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
        retransmissionService.validateRequest(request);
        permissions.ensurePermissionsToBothTopics(request, requestContext);
        retransmissionService.createTask(request);
        auditor.auditRetransmissionCreation(request, requestContext);
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
            Topic sourceTopic = topicRepository.getTopicDetails(TopicName.fromQualifiedName(request.getSourceTopic()));
            Topic targetTopic = topicRepository.getTopicDetails(TopicName.fromQualifiedName(request.getTargetTopic()));
            boolean hasPermissions = managementRights.isUserAllowedToManageTopic(sourceTopic, requestContext)
                    && managementRights.isUserAllowedToManageTopic(targetTopic, requestContext);
            if (!hasPermissions) {
                throw new PermissionDeniedException("User needs permissions to source and target topics.");
            }
        }
    }

    private static class OfflineRetransmissionAuditor {
        private static final Logger logger = LoggerFactory.getLogger(OfflineRetransmissionAuditor.class);

        public void auditRetransmissionCreation(OfflineRetransmissionRequest request, ContainerRequestContext requestContext) {
            String username = extractUsername(requestContext);
            logger.info("User {} created retransmission task: {}", username, request);
        }

        private String extractUsername(ContainerRequestContext requestContext) {
            return requestContext.getSecurityContext().getUserPrincipal().getName();
        }
    }
}

