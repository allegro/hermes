package pl.allegro.tech.hermes.consumers.utils;

import com.google.common.net.HostAndPort;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class HostUtilsTest {

    @Test
    public void shouldReadHostAndPorts() throws Exception {
        String hostsWithPorts = "localhost:8012,allegro.pl:9043";

        List<HostAndPort> result = HostUtils.readHostsAndPorts(hostsWithPorts);

        assertThat(result)
            .contains(HostAndPort.fromParts("localhost", 8012))
            .contains(HostAndPort.fromParts("allegro.pl", 9043));
    }
}