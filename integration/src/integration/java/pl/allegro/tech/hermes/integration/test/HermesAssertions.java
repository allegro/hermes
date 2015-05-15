package pl.allegro.tech.hermes.integration.test;

import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.api.Assertions;

import javax.ws.rs.core.Response;

public final class HermesAssertions extends Assertions {

    private HermesAssertions() {
    }

    public static HttpResponseAssertion assertThat(Response response) {
        return new HttpResponseAssertion(response);
    }
    public static ZookeeperAssertion assertThat(CuratorFramework zookeeper) {
        return new ZookeeperAssertion(zookeeper, ZookeeperAssertion.class);
    }
}
