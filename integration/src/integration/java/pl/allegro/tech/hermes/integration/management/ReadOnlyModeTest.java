package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.management.TestSecurityProvider;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

import javax.ws.rs.core.Response;

import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group;

public class ReadOnlyModeTest extends IntegrationTest {

    @BeforeMethod
    public void initialize() {
        TestSecurityProvider.setUserIsAdmin(true);
        management.modeEndpoint().setMode(ModeService.READ_WRITE);
    }

    @AfterMethod
    public void cleanup() {
        TestSecurityProvider.setUserIsAdmin(true);
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
    public void shouldRestrictModifyingOperationsForNonAdminUsers() {
        // given
        management.modeEndpoint().setMode(ModeService.READ_ONLY);
        String groupName = "not-allowed-group";
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        Response response = createGroup(groupName);

        // then
        assertThat(response).hasStatus(Response.Status.SERVICE_UNAVAILABLE);
    }

    @Test
    public void shouldNotRestrictModifyingOperationsForAdminUsers() {
        // given
        management.modeEndpoint().setMode(ModeService.READ_ONLY);
        String groupName = "not-allowed-group";
        TestSecurityProvider.setUserIsAdmin(true);

        // when
        Response response = createGroup(groupName);

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
    }

    @Test
    public void shouldSwitchModeBack() {
        // given
        management.modeEndpoint().setMode(ModeService.READ_ONLY);
        String groupName = "not-allowed-at-first-group";
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        Response response = createGroup(groupName);

        // then
        assertThat(response).hasStatus(Response.Status.SERVICE_UNAVAILABLE);

        // and
        TestSecurityProvider.setUserIsAdmin(true);
        management.modeEndpoint().setMode(ModeService.READ_WRITE);
        TestSecurityProvider.setUserIsAdmin(false);

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
