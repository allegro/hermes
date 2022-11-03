package pl.allegro.tech.hermes.integration.management;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Owner;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.management.domain.owner.CrowdOwnerSource;

import java.util.List;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class CrowdOwnerSourceIntegrationTest extends IntegrationTest {

    private static final int SERVICE_PORT = 18222;

    private static final String BASE_API_PATH = "/crowd/rest/usermanagement/1/search";

    private String firstTeam = "Scrum A";

    private String secondTeam = "Scrum B";

    private WireMock wireMock;

    private WireMockServer server;

    @BeforeClass
    public void initialize() {
        wireMock = new WireMock("localhost", SERVICE_PORT);
        server = new WireMockServer(SERVICE_PORT);
        server.start();
    }

    @AfterClass
    public void cleanUp() {
        server.stop();
    }

    @Test
    public void shouldCrowdServiceBeCalledOnce() {
        //given
        wireMock.register(get(urlMatching(BASE_API_PATH + ".*"))
            .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .withBody(String.format("{ \"expand\": \"group\", \"groups\": [ %s,%s ]}",
                            groupResponse("http://main/crowd/groupname/A", firstTeam),
                            groupResponse("http://main/crowd/groupname/B", secondTeam)))));


        //when
        management.owner().search(CrowdOwnerSource.NAME, "Scrum");

        //then
        wireMock.verifyThat(1, getRequestedFor(urlMatching(BASE_API_PATH + ".*")));
    }

    @Test
    public void shouldReturnTwoResultsFromCrowd() {
        //given
        wireMock.register(get(urlMatching(BASE_API_PATH + ".*"))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .withBody(String.format("{ \"expand\": \"group\", \"groups\": [ %s,%s ]}",
                                groupResponse("http://main/crowd/groupname/A", firstTeam),
                                groupResponse("http://main/crowd/groupname/B", secondTeam)))));


        //when
        List<Owner> groups = management.owner().search(CrowdOwnerSource.NAME, "Scrum");

        //then
        assertThat(groups.stream().map(Owner::getId)).containsExactly(firstTeam, secondTeam);
    }

    @Test
    public void shouldReturnNoResultsFromCrowd() {
        //given
        wireMock.register(get(urlMatching(BASE_API_PATH + ".*"))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .withBody("{ \"expand\": \"group\", \"groups\": []}")));


        //when
        List<Owner> groups = management.owner().search(CrowdOwnerSource.NAME, "Non Matching");

        //then
        assertThat(groups).isEmpty();
    }

    @Test
    public void shouldGetAnExceptionOnReadTimeout() {
        //given
        wireMock.register(get(urlMatching(BASE_API_PATH + ".*"))
                .withHeader(HttpHeaders.ACCEPT, equalTo(MediaType.APPLICATION_JSON))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .withBody("{ \"expand\": \"group\", \"groups\": []}")
                        .withFixedDelay(3000)));


        //when
        InternalServerErrorException e = (InternalServerErrorException) catchThrowable(
                () -> management.owner().search(CrowdOwnerSource.NAME, "Non Matching")
        );

        //then
        assertThat(e.getResponse()).hasErrorCode(ErrorCode.CROWD_GROUPS_COULD_NOT_BE_LOADED);
    }

    private String groupResponse(String path, String name) {
        return String.format("{ \"link\": { \"href\": \"%s\", \"rel\": \"self\" }, \"name\": \"%s\"}", path, name);
    }
}
