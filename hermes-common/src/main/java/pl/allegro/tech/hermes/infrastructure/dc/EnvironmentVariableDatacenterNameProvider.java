package pl.allegro.tech.hermes.infrastructure.dc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentVariableDatacenterNameProvider implements DatacenterNameProvider {
  private static final Logger logger =
      LoggerFactory.getLogger(EnvironmentVariableDatacenterNameProvider.class);

  private final String variableName;

  public EnvironmentVariableDatacenterNameProvider(String variableName) {
    this.variableName = variableName;
  }

  @Override
  public String getDatacenterName() {
    String dcName = System.getenv(variableName);
    if (dcName == null) {
      logger.info("Undefined environment variable: " + variableName);
      throw new DcNameProvisionException("Undefined environment variable: " + variableName);
    }
    logger.info("Providing DC name from environment variable: {}={}", variableName, dcName);
    return dcName;
  }
}
