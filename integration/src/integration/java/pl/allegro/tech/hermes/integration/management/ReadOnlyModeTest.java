package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group;

import javax.ws.rs.core.Response;

public class ReadOnlyModeTest extends IntegrationTest {

    @BeforeMethod
    public void initialize() {
        management.modeEndpoint().setMode(ModeService.READ_WRITE);
    }

    @Test
    public void shouldAllowNonModifyingOperations() {
        // given
        management.modeEndpoint().setMode(ModeService.READ_WRITE);
        String groupName = "allowed-group";

        // when
        Response response = createGroup(groupName);

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
    }

    @Test
    public void shouldRestrictModifyingOperations() {
        // given
        management.modeEndpoint().setMode(ModeService.READ_ONLY);
        String groupName = "not-allowed-group";

        // when
        Response response = createGroup(groupName);

        // then
        assertThat(response).hasStatus(Response.Status.SERVICE_UNAVAILABLE);
    }

    @Test
    public void shouldSwitchModeBack() {
        // given
        management.modeEndpoint().setMode(ModeService.READ_ONLY);
        String groupName = "not-allowed-at-first-group";

        // when
        Response response = createGroup(groupName);

        // then
        assertThat(response).hasStatus(Response.Status.SERVICE_UNAVAILABLE);

        // and
        management.modeEndpoint().setMode(ModeService.READ_WRITE);

        // when
        response = createGroup(groupName);

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
    }

    public Response createGroup(String groupName) {
        Group group = group(groupName).build();
        return management.group().create(group);
    }
}
