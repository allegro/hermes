package pl.allegro.tech.hermes.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class HostnameResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostnameResolver.class);

    private HostnameResolver() { }

    public static String detectHostname() {
        String hostname = "hostname-could-not-be-detected";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            LOGGER.warn("Could not determine hostname");
        }
        return hostname;
    }

}
