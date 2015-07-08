package pl.allegro.tech.hermes.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class InetAddressHostnameResolver implements HostnameResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(InetAddressHostnameResolver.class);

    public InetAddressHostnameResolver() { }

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
