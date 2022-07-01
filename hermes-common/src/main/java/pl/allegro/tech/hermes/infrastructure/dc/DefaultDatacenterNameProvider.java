package pl.allegro.tech.hermes.infrastructure.dc;

public class DefaultDatacenterNameProvider implements DatacenterNameProvider {

    public static final String DEFAULT_DC_NAME = "dc";

    @Override
    public String getDatacenterName() {
        return DEFAULT_DC_NAME;
    }
}
