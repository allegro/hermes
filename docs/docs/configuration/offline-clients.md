# Offline Clients

This feature lets you display offline clients of topics. If your topics are read in external way (e.g. messages getting transferred to Hadoop and analysed there by users), you can provide the source for an inline iframe that will be used to show this information in Console.

## How to enable

The `topic.offlineClientsEnabled` property in Console should be enabled. (see [console section](http://hermes-pubsub.readthedocs.io/en/latest/configuration/console/#topic-configuration) for more information.)

The first step is to create bean in Management module context that implements `pl.allegro.tech.hermes.management.domain.clients.OfflineClientsService` interface (see [packaging section](../deployment/packaging.md#management) for more information on Management customization).
The bean should implement building source for iframe, which should be displayed in console.

The clients should now be displayed in topic details view in Console.
