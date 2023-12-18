package pl.allegro.tech.hermes.integrationtests.management;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Group;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.management.TestSecurityProvider;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

public class ReadOnlyModeTest {

    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @BeforeEach
    public void initialize() {
        TestSecurityProvider.setUserIsAdmin(true);
        hermes.api().setMode(ModeService.READ_WRITE);
    }

    @AfterEach
    public void cleanup() {
        TestSecurityProvider.setUserIsAdmin(true);
    }

    @Test
    public void shouldAllowNonModifyingOperations() {
        // given
        hermes.api().setMode(ModeService.READ_WRITE);
        String groupName = "allowed-group";

        // when
        WebTestClient.ResponseSpec response = hermes.api().createGroup(Group.from(groupName));

        // then
        response.expectStatus().isCreated();
    }

    @Test
    public void shouldRestrictModifyingOperationsForNonAdminUsers() {
        // given
        hermes.api().setMode(ModeService.READ_ONLY);
        String groupName = "not-allowed-group";
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        WebTestClient.ResponseSpec response = hermes.api().createGroup(Group.from(groupName));

        // then
        response.expectStatus().is5xxServerError();
    }

    @Test
    public void shouldNotRestrictModifyingOperationsForAdminUsers() {
        // given
        hermes.api().setMode(ModeService.READ_ONLY);
        String groupName = "not-allowed-group2";
        TestSecurityProvider.setUserIsAdmin(true);

        // when
        WebTestClient.ResponseSpec response = hermes.api().createGroup(Group.from(groupName));

        // then
        response.expectStatus().isCreated();
    }

    @Test
    public void shouldSwitchModeBack() {
        // given
        hermes.api().setMode(ModeService.READ_ONLY);
        String groupName = "not-allowed-at-first-group";
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        WebTestClient.ResponseSpec response = hermes.api().createGroup(Group.from(groupName));

        // then
        response.expectStatus().is5xxServerError();

        // and
        TestSecurityProvider.setUserIsAdmin(true);
        hermes.api().setMode(ModeService.READ_WRITE).expectStatus().isOk();
        TestSecurityProvider.setUserIsAdmin(false);

        // when
        response = hermes.api().createGroup(Group.from(groupName));

        // then
        response.expectStatus().isCreated();
    }
}
