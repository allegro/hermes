package pl.allegro.tech.hermes.management.domain.supportTeam;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SupportTeam;
import pl.allegro.tech.hermes.management.config.SupportTeamServiceProperties;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class SupportTeamCache {

    private final LoadingCache<String, List<SupportTeam>> cache;

    @Autowired
    public SupportTeamCache(SupportTeamService supportTeamService, SupportTeamServiceProperties supportTeamServiceProperties) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(supportTeamServiceProperties.getCacheDurationSeconds(), TimeUnit.SECONDS)
                .maximumSize(supportTeamServiceProperties.getCacheSize())
                .build(new CacheLoader<String, List<SupportTeam>>() {
            @Override
            public List<SupportTeam> load(String searchString) throws Exception {
                return supportTeamService.getSupportTeams(searchString);
            }
        });
    }

    public List<SupportTeam> getSupportTeams(String searchString) {
        try {
            return cache.get(searchString);
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw new CouldNotLoadSupportTeamsException(e);
        }
    }
}

