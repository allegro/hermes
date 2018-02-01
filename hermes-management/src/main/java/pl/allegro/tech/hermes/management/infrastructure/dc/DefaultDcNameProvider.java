package pl.allegro.tech.hermes.management.infrastructure.dc;

public class DefaultDcNameProvider implements DcNameProvider {
    @Override
    public String getDcName() {
        return "default";
    }
}
