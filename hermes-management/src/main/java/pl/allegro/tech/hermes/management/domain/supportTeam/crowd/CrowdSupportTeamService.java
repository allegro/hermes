package pl.allegro.tech.hermes.management.domain.supportTeam.crowd;

import com.google.common.io.BaseEncoding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import pl.allegro.tech.hermes.api.CrowdGroups;
import pl.allegro.tech.hermes.api.SupportTeam;
import pl.allegro.tech.hermes.management.config.SupportTeamServiceProperties;
import pl.allegro.tech.hermes.management.domain.supportTeam.SupportTeamService;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CrowdSupportTeamService implements SupportTeamService {

    private static final String CROWD_API_SUFFIX = "/rest/usermanagement/1";

    private static final String GROUP_SEARCH_SUFFIX = "/search?entity-type=group&restriction=name=\"*{groupName}*\"";

    private final RestTemplate restTemplate;

    private final SupportTeamServiceProperties.CrowdProperties crowdProperties;

    private final HttpEntity<Void> entity;

    private final String groupSearchUrl;

    @Autowired
    public CrowdSupportTeamService(RestTemplate restTemplate, SupportTeamServiceProperties supportTeamServiceProperties) {
        this.restTemplate = restTemplate;
        this.crowdProperties = supportTeamServiceProperties.getCrowd();
        this.groupSearchUrl = crowdProperties.getPath() + CROWD_API_SUFFIX + GROUP_SEARCH_SUFFIX;
        this.entity = configureEntity();
    }

    private HttpEntity<Void> configureEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.AUTHORIZATION, buildBasicAuthorizationValue());
        return new HttpEntity<>(headers);
    }

    private String buildBasicAuthorizationValue() {
        String encodedValue = String.join(":", crowdProperties.getUserName(), crowdProperties.getPassword());
        return "Basic " + BaseEncoding.base64().encode(encodedValue.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public List<SupportTeam> getSupportTeams(String searchString) {
        CrowdGroups crowdGroups = restTemplate.exchange(this.groupSearchUrl, HttpMethod.GET, this.entity, CrowdGroups.class, searchString)
                .getBody();
        return crowdGroups.getCrowdGroupDescriptions().stream()
                .map(description -> new SupportTeam(description.getName()))
                .collect(Collectors.toList());
    }
}
