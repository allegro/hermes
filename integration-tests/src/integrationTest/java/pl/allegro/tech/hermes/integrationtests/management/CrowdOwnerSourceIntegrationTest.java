package pl.allegro.tech.hermes.integrationtests.management;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Owner;
import pl.allegro.tech.hermes.integrationtests.setup.CrowdExtension;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.management.domain.owner.CrowdOwnerSource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CrowdOwnerSourceIntegrationTest {

    @Order(0)
    @RegisterExtension
    public static final CrowdExtension crowd = new CrowdExtension();

    @Order(1)
    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension()
            .withCrowd(crowd);

    @Test
    public void shouldCrowdServiceBeCalledOnce() {
        //given
        crowd.stubGroups("Scrum A", "Scrum B");

        //when
        hermes.api().searchOwners(CrowdOwnerSource.NAME, "Scrum");

        //then
        crowd.assertRequestCount(1);
    }

    @Test
    public void shouldReturnTwoResultsFromCrowd() {
        //given
        crowd.stubGroups("Scrum A", "Scrum B");

        //when
        WebTestClient.ResponseSpec response =  hermes.api().searchOwners(CrowdOwnerSource.NAME, "Scrum");

        //then
        List<Owner> groups = response
                .expectStatus().isOk()
                .expectBodyList(Owner.class).returnResult().getResponseBody();
        assertThat(groups).isNotNull();
        assertThat(groups.stream().map(Owner::getId)).containsExactly("Scrum A", "Scrum B");
    }

    @Test
    public void shouldReturnNoResultsFromCrowd() {
        //given
        crowd.stubGroups();

        //when
        WebTestClient.ResponseSpec response =  hermes.api().searchOwners(CrowdOwnerSource.NAME, "Non Matching");

        //then
        response.expectStatus().isOk()
                .expectBodyList(Owner.class).hasSize(0);
    }

    @Test
    public void shouldGetAnExceptionOnReadTimeout() {
        //given
        crowd.stubDelay(Duration.ofMillis(3500));

        //when
        WebTestClient.ResponseSpec response =  hermes.api().searchOwners(CrowdOwnerSource.NAME, "Non Matching");

        //then
        ErrorDescription error = response
                .expectStatus().is5xxServerError()
                .expectBody(ErrorDescription.class).returnResult().getResponseBody();
        assertThat(error).isNotNull();
        assertThat(error.getCode()).isEqualTo(ErrorCode.CROWD_GROUPS_COULD_NOT_BE_LOADED);
    }
}
