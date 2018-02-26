package pl.allegro.tech.hermes.management.infrastructure.dc;

public class DefaultDcNameProvider implements DcNameProvider {

    public static final String DEFAULT_DC_NAME = "default";

    @Override
    public String getDcName() {
        return DEFAULT_DC_NAME;
    }
}
