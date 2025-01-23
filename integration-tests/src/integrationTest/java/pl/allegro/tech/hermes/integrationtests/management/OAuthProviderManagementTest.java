package pl.allegro.tech.hermes.integrationtests.management;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.PatchData.patchData;
import static pl.allegro.tech.hermes.test.helper.builder.OAuthProviderBuilder.oAuthProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient.ResponseSpec;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.OAuthProvider;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

public class OAuthProviderManagementTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  @Test
  public void shouldCreateOAuthProvider() {
    // given
    OAuthProvider oAuthProvider = oAuthProvider("myProvider").withSocketTimeout(200).build();

    // when
    ResponseSpec response = hermes.api().createOAuthProvider(oAuthProvider);

    // then
    response.expectStatus().isCreated();
    OAuthProvider createdProvider =
        hermes
            .api()
            .getOAuthProvider(oAuthProvider.getName())
            .expectStatus()
            .isOk()
            .expectBody(OAuthProvider.class)
            .returnResult()
            .getResponseBody();
    assertThat(createdProvider).isNotNull();
    assertThat(createdProvider.getSocketTimeout()).isEqualTo(200);
  }

  @Test
  public void shouldNotAllowCreatingTheSameOAuthProviderTwice() {
    // given
    OAuthProvider oAuthProvider = oAuthProvider("originalProvider").build();
    hermes.initHelper().createOAuthProvider(oAuthProvider);

    // when
    ResponseSpec secondTryResponse = hermes.api().createOAuthProvider(oAuthProvider);

    // then
    ErrorDescription error =
        secondTryResponse
            .expectStatus()
            .isBadRequest()
            .expectBody(ErrorDescription.class)
            .returnResult()
            .getResponseBody();
    assertThat(error).isNotNull();
    assertThat(error.getCode()).isEqualTo(ErrorCode.OAUTH_PROVIDER_ALREADY_EXISTS);
  }

  @Test
  public void shouldUpdateOAuthProvider() {
    // given
    OAuthProvider oAuthProvider = oAuthProvider("myOtherProvider").build();
    hermes.initHelper().createOAuthProvider(oAuthProvider);

    // when
    PatchData patch =
        patchData()
            .set("tokenEndpoint", "http://other.example.com/other")
            .set("socketTimeout", 100)
            .build();
    ResponseSpec updateResponse = hermes.api().updateOAuthProvider(oAuthProvider.getName(), patch);

    // then
    updateResponse.expectStatus().isOk();
    OAuthProvider updatedProvider =
        hermes
            .api()
            .getOAuthProvider(oAuthProvider.getName())
            .expectStatus()
            .isOk()
            .expectBody(OAuthProvider.class)
            .returnResult()
            .getResponseBody();
    assertThat(updatedProvider).isNotNull();
    assertThat(updatedProvider.getTokenEndpoint()).isEqualTo("http://other.example.com/other");
    assertThat(updatedProvider.getSocketTimeout()).isEqualTo(100);
  }

  @Test
  public void shouldRemoveOAuthProvider() {
    // given
    OAuthProvider oAuthProvider = oAuthProvider("myProviderForRemoval").build();
    hermes.initHelper().createOAuthProvider(oAuthProvider);

    // when
    ResponseSpec removalResponse = hermes.api().removeOAuthProvider(oAuthProvider.getName());

    // then
    removalResponse.expectStatus().isOk();
    hermes
        .api()
        .listOAuthProvider()
        .expectStatus()
        .isOk()
        .expectBodyList(String.class)
        .doesNotContain(oAuthProvider.getName());
  }
}
