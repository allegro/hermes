package pl.allegro.tech.hermes.integrationtests.management;

import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.groupWithRandomName;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.management.TestSecurityProvider;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

public class ReadOnlyModeTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

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
  public void shouldAllowModifyingOperations() {
    // given
    hermes.api().setMode(ModeService.READ_WRITE);
    TestSecurityProvider.setUserIsAdmin(false);

    // when
    WebTestClient.ResponseSpec response = hermes.api().createGroup(groupWithRandomName().build());

    // then
    response.expectStatus().isCreated();
  }

  @Test
  public void shouldRestrictModifyingOperationsForNonAdminUsers() {
    // given
    hermes.api().setMode(ModeService.READ_ONLY);
    TestSecurityProvider.setUserIsAdmin(false);

    // when
    WebTestClient.ResponseSpec response = hermes.api().createGroup(groupWithRandomName().build());

    // then
    response.expectStatus().isEqualTo(503);
  }

  @Test
  public void shouldNotRestrictModifyingOperationsForAdminUsers() {
    // given
    hermes.api().setMode(ModeService.READ_ONLY);
    TestSecurityProvider.setUserIsAdmin(true);

    // when
    WebTestClient.ResponseSpec response = hermes.api().createGroup(groupWithRandomName().build());

    // then
    response.expectStatus().isCreated();
  }

  @Test
  public void shouldSwitchModeBack() {
    // given
    hermes.api().setMode(ModeService.READ_ONLY);
    TestSecurityProvider.setUserIsAdmin(false);

    // when
    WebTestClient.ResponseSpec response = hermes.api().createGroup(groupWithRandomName().build());

    // then
    response.expectStatus().isEqualTo(503);

    // and
    TestSecurityProvider.setUserIsAdmin(true);
    hermes.api().setMode(ModeService.READ_WRITE).expectStatus().isOk();
    TestSecurityProvider.setUserIsAdmin(false);

    // when
    response = hermes.api().createGroup(groupWithRandomName().build());

    // then
    response.expectStatus().isCreated();
  }
}
