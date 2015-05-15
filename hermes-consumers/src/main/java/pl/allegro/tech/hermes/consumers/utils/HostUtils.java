package pl.allegro.tech.hermes.consumers.utils;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

import java.util.List;

public class HostUtils {
    private static final String HOSTS_SEPARATOR = ",";

    public static List<HostAndPort> readHostsAndPorts(String hostsAndPorts) {
        List<HostAndPort> result = Lists.newArrayList();

        String [] hosts = hostsAndPorts.split(HOSTS_SEPARATOR);
        for (String host: hosts) {
            result.add(HostAndPort.fromString(host));
        }

        return result;
    }
}
