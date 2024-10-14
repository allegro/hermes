package pl.allegro.tech.hermes.benchmark.environment;

import pl.allegro.tech.hermes.frontend.publishing.preview.MessagePreviewPersister;

class NoOpMessagePreviewPersister implements MessagePreviewPersister {

  @Override
  public void start() {}

  @Override
  public void shutdown() {}
}
