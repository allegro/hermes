package pl.allegro.tech.hermes.infrastructure.dc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDatacenterNameProvider implements DatacenterNameProvider {

  private static final Logger logger = LoggerFactory.getLogger(DefaultDatacenterNameProvider.class);

  public static final String DEFAULT_DC_NAME = "dc";

  @Override
  public String getDatacenterName() {
    logger.info("Providing default datacenter name: {}", DEFAULT_DC_NAME);
    return DEFAULT_DC_NAME;
  }
}
