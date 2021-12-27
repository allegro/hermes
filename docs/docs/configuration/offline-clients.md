# Offline Clients

This feature lets you display offline clients of topics. If your topics are read in external way (e.g. messages getting transferred to Hadoop and analysed there by users), you can provide an inline iframe that will be used to show this information in Console.

## How to enable

The `topic.offlineClientsEnabled` property in Console should be enabled. (see [console section](http://hermes-pubsub.readthedocs.io/en/latest/configuration/console/#topic-configuration) for more information.)

Then you should provide source for iframe within property `topic.offlineClientsIframeSource`, for example link to the dashboard from Data Studio.

The clients should now be displayed in topic details view in Console.
