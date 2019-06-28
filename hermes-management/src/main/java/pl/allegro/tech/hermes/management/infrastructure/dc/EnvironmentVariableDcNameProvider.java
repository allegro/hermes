package pl.allegro.tech.hermes.management.infrastructure.dc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvironmentVariableDcNameProvider implements DcNameProvider {
    private static final Logger logger = LoggerFactory.getLogger(EnvironmentVariableDcNameProvider.class);

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
        logger.info("Providing DC name from environment variable: {}={}", variableName, dcName);
        return dcName;
    }
}
