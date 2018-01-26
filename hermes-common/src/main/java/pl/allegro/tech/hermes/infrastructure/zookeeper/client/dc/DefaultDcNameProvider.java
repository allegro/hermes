package pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc;

public class DefaultDcNameProvider implements DcNameProvider {
    @Override
    public String getDcName() {
        return "default";
    }
}
