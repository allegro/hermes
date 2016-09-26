package pl.allegro.tech.hermes.management.domain.supportTeam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.management.config.SupportTeamServiceProperties;
import pl.allegro.tech.hermes.management.domain.supportTeam.crowd.CrowdSupportTeamService;

import java.util.Optional;

@Component
public class SupportTeamServiceFactory extends AbstractFactoryBean<SupportTeamService> {

    private final RestTemplate restTemplate;

    private final SupportTeamServiceProperties supportTeamServiceProperties;

    @Autowired
    public SupportTeamServiceFactory(RestTemplate restTemplate, SupportTeamServiceProperties supportTeamServiceProperties) {
        this.restTemplate = restTemplate;
        this.supportTeamServiceProperties = supportTeamServiceProperties;
    }

    @Override
    public Class<?> getObjectType() {
        return SupportTeamService.class;
    }

    @Override
    protected SupportTeamService createInstance() throws Exception {
        return supportTeamServiceProperties.getType()
                .flatMap(type -> supportTeamServiceProperties.isEnabled() ?
                        Optional.of(createService(type)) : Optional.empty())
                .orElseGet(NoOperationSupportTeamService::new);
    }

    private SupportTeamService createService(SupportTeamServiceProperties.SupportTeamServiceType type) {
        switch (type) {
            case CROWD:
                return new CrowdSupportTeamService(restTemplate, supportTeamServiceProperties);
            default:
                return new NoOperationSupportTeamService();
        }
    }
}
