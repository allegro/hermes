package pl.allegro.tech.hermes.management.api;

import com.google.common.collect.ImmutableList;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSourceNotFound;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSources;
import pl.allegro.tech.hermes.management.migration.maintainer.SupportTeamToMaintainerMigrator;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/migrations")
@RolesAllowed(Roles.ADMIN)
@Api(value = "/migrations", description = "Storage schema & data migrations")
public class MigrationsEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(MigrationsEndpoint.class);
    private static final String SUPPORT_TEAM_TO_MAINTAINER_MIGRATION_NAME = "support-team-to-maintainer";

    private final SupportTeamToMaintainerMigrator supportTeamToMaintainerMigrator;
    private final MaintainerSources maintainerSources;

    @Autowired
    public MigrationsEndpoint(SupportTeamToMaintainerMigrator supportTeamToMaintainerMigrator,
                              MaintainerSources maintainerSources) {
        this.supportTeamToMaintainerMigrator = supportTeamToMaintainerMigrator;
        this.maintainerSources = maintainerSources;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List possible migrations", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> list() {
        return ImmutableList.of(SUPPORT_TEAM_TO_MAINTAINER_MIGRATION_NAME);
    }

    @POST
    @Produces(APPLICATION_JSON)
    @Path("/{name}")
    @ApiOperation(value = "Execute migration", httpMethod = HttpMethod.POST)
    public SupportTeamToMaintainerMigrator.ExecutionStats execute(@PathParam("name") String name,
                                                                  @QueryParam("source") String sourceName,
                                                                  @DefaultValue("false") @QueryParam("override") boolean overrideMaintainers,
                                                                  @Context SecurityContext securityContext) {
        if (SUPPORT_TEAM_TO_MAINTAINER_MIGRATION_NAME.equals(name)) {
            maintainerSources.getByName(sourceName).orElseThrow(() -> new MaintainerSourceNotFound(sourceName));
            logger.info("{} migration triggered by user {}", SUPPORT_TEAM_TO_MAINTAINER_MIGRATION_NAME, securityContext.getUserPrincipal().getName());
            return supportTeamToMaintainerMigrator.execute(sourceName,
                    overrideMaintainers ? SupportTeamToMaintainerMigrator.MaintainerExistsStrategy.OVERRIDE : SupportTeamToMaintainerMigrator.MaintainerExistsStrategy.SKIP
            );
        }

        throw new UnknownMigrationException(name);
    }

    private static class UnknownMigrationException extends HermesException {

        UnknownMigrationException(String migrationName) {
            super("Unknown migration '" + migrationName + "'");
        }

        @Override
        public ErrorCode getCode() {
            return ErrorCode.UNKNOWN_MIGRATION;
        }
    }

}
