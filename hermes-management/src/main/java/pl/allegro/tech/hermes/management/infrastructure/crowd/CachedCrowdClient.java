package pl.allegro.tech.hermes.management.infrastructure.crowd;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import pl.allegro.tech.hermes.management.config.CrowdProperties;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class CachedCrowdClient implements CrowdClient {

    private final LoadingCache<String, List<String>> cache;

    public CachedCrowdClient(CrowdClient crowdClient, CrowdProperties crowdProperties) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterWrite(crowdProperties.getCacheDurationSeconds(), TimeUnit.SECONDS)
                .maximumSize(crowdProperties.getCacheSize())
                .build(new CacheLoader<String, List<String>>() {
                    @Override
                    public List<String> load(String searchString) throws Exception {
                        return crowdClient.getGroups(searchString);
                    }
                });
    }

    @Override
    public List<String> getGroups(String searchString) {
        try {
            return cache.get(searchString);
        } catch (ExecutionException | UncheckedExecutionException e) {
            throw new CouldNotLoadCrowdGroupsException(e);
        }
    }
}

