package pl.allegro.tech.hermes.consumers.consumer.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.OAuthProvider;

public class OAuthProvidersNotifyingCache extends PathChildrenCache
    implements PathChildrenCacheListener {

  private static final Logger logger = LoggerFactory.getLogger(OAuthProvidersNotifyingCache.class);

  private final ObjectMapper objectMapper;

  private OAuthProviderCacheListener listener;

  public OAuthProvidersNotifyingCache(
      CuratorFramework curator,
      String path,
      ExecutorService executorService,
      ObjectMapper objectMapper) {
    super(curator, path, true, false, executorService);
    this.objectMapper = objectMapper;
    getListenable().addListener(this);
  }

  @Override
  public void start() throws Exception {
    super.start();
  }

  public void setListener(OAuthProviderCacheListener listener) {
    this.listener = listener;
  }

  @Override
  public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
    if (event.getData() == null || event.getData().getData() == null) {
      return;
    }
    if (event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED) {
      parseEvent(event.getData().getPath(), event.getData().getData())
          .ifPresent(listener::oAuthProviderUpdate);
    }
  }

  private Optional<OAuthProvider> parseEvent(String path, byte[] data) {
    try {
      return Optional.of(objectMapper.readValue(data, OAuthProvider.class));
    } catch (IOException e) {
      logger.error("Failed to parse object at path {}", path);
      return Optional.empty();
    }
  }
}
