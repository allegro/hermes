package pl.allegro.tech.hermes.infrastructure.zookeeper.client.dc;

public class EnvironmentVariableDcNameProvider implements DcNameProvider {

    private String variableName;

    public EnvironmentVariableDcNameProvider(String variableName) {
        this.variableName = variableName;
    }

    @Override
    public String getDcName() {
        String dcName = System.getenv(variableName);
        if(dcName == null) {
            throw new DcNameProvisionException("Undefined environment variable: " + variableName);
        }
        return dcName;
    }
}
