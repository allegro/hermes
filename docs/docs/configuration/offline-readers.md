# Offline Readers

This feature lets you display offline readers of topics. If your topics are read in external way, you can provide implementation that will be used to show this information in Console.

## How to enable

The first step is to create bean in Management module context that implements `pl.allegro.tech.hermes.management.domain.readers.OfflineReadersService` interface (see [packaging section](/deployment/packaging#management) for more information on Management customization).
The bean should implement fetching of the readers, which could be for example rest request for another service.

Then the property `topic.offlineReadersEnabled` in Console should be enabled. (see [console section](http://hermes-pubsub.readthedocs.io/en/latest/configuration/console/#topic-configuration) for more information.)

The readers should now be displayed in topic details view in Console.
