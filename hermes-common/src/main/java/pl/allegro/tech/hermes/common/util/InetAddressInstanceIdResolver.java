package pl.allegro.tech.hermes.common.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InetAddressInstanceIdResolver implements InstanceIdResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(InetAddressInstanceIdResolver.class);

  public InetAddressInstanceIdResolver() {}

  public String resolve() {
    String hostname = "hostname-could-not-be-detected";
    try {
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      LOGGER.warn("Could not determine hostname");
    }
    return hostname;
  }
}
