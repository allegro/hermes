package pl.allegro.tech.hermes.consumers.uri;

import static java.net.URI.create;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.consumers.uri.UriUtils.appendContext;
import static pl.allegro.tech.hermes.consumers.uri.UriUtils.extractAddressFromUri;
import static pl.allegro.tech.hermes.consumers.uri.UriUtils.extractContextFromUri;
import static pl.allegro.tech.hermes.consumers.uri.UriUtils.extractHostFromUri;
import static pl.allegro.tech.hermes.consumers.uri.UriUtils.extractPasswordFromUri;
import static pl.allegro.tech.hermes.consumers.uri.UriUtils.extractPortFromUri;
import static pl.allegro.tech.hermes.consumers.uri.UriUtils.extractUserNameFromUri;

import java.net.URI;
import org.junit.Test;

public class UriUtilsTest {

  private static final URI FULL_URI =
      create("jms://user:pass@localhost:123123/12312312/312?param=test");
  private static final URI NO_PORT_URI =
      create("jms://user:pass@localhost/12312312/312?param=test");
  private static final URI NO_USER_URI = create("jms://localhost:123123/12312312/312?param=test");

  @Test(expected = InvalidHostException.class)
  public void shouldThrowExceptionForInvalidHost() {
    extractHostFromUri(URI.create("jms://host_with_underscores/test"));
  }

  @Test
  public void shouldExtractHostFromUri() {
    assertThat(extractHostFromUri(FULL_URI)).isEqualTo("localhost");
  }

  @Test
  public void shouldExtractPortFromUri() {
    assertThat(extractPortFromUri(FULL_URI).longValue()).isEqualTo(123123L);
    assertThat(extractPortFromUri(NO_PORT_URI)).isNull();
  }

  @Test
  public void shouldExtractAddressFromUri() {
    assertThat(extractAddressFromUri(FULL_URI)).isEqualTo("localhost:123123");
    assertThat(extractAddressFromUri(NO_PORT_URI)).isEqualTo("localhost");
  }

  @Test
  public void shouldExtractUsernameFromUri() {
    assertThat(extractUserNameFromUri(FULL_URI)).isEqualTo("user");
    assertThat(extractUserNameFromUri(NO_USER_URI)).isNull();
  }

  @Test
  public void shouldExtractPasswordFromUri() {
    assertThat(extractPasswordFromUri(FULL_URI)).isEqualTo("pass");
    assertThat(extractPasswordFromUri(NO_USER_URI)).isNull();
  }

  @Test
  public void shouldExtractContextFromUri() {
    assertContext("http://localhost:8080", "");
    assertContext("http://localhost:8080/", "/");
    assertContext("http://localhost:8080/path/1", "/path/1");
    assertContext(
        "http://localhost:8080/path/1?arg1=test1&arg2=test2", "/path/1?arg1=test1&arg2=test2");
    assertContext("http://localhost:8080/path/1?arg=test#fragment", "/path/1?arg=test#fragment");
    assertContext("http://localhost:8080?arg=test", "?arg=test");
    assertContext("http://localhost:8080?arg=test#fragment", "?arg=test#fragment");
    assertContext("http://localhost:8080#fragment", "#fragment");
  }

  private void assertContext(String uri, String context) {
    assertThat(extractContextFromUri(create(uri))).isEqualTo(context);
  }

  @Test
  public void shouldAppendContextToURI() {
    // given when then
    assertThat(appendContext(URI.create("http://localhost:8080/"), "context"))
        .isEqualTo(URI.create("http://localhost:8080/context"));

    assertThat(appendContext(URI.create("http://localhost:8080/"), "/context"))
        .isEqualTo(URI.create("http://localhost:8080/context"));

    assertThat(appendContext(URI.create("http://localhost:8080/sth"), "context"))
        .isEqualTo(URI.create("http://localhost:8080/sth/context"));
  }
}
