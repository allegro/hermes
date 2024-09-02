package pl.allegro.tech.hermes.mock;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class HermesMockExtension
    implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

  private final HermesMock hermesMock;

  public HermesMockExtension(int port) {
    this(new HermesMock.Builder().withPort(port).build());
  }

  public HermesMockExtension(HermesMock hermesMock) {
    this.hermesMock = hermesMock;
  }

  public HermesMockDefine define() {
    return hermesMock.define();
  }

  public HermesMockExpect expect() {
    return hermesMock.expect();
  }

  public HermesMockQuery query() {
    return hermesMock.query();
  }

  public void resetReceivedRequest() {
    hermesMock.resetReceivedRequest();
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    hermesMock.start();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    hermesMock.resetReceivedRequest();
    hermesMock.stop();
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().isAssignableFrom(HermesMock.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return hermesMock;
  }
}
