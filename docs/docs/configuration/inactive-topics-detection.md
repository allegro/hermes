# Inactive Topics Detection

Hermes Management provides an optional feature to detect inactive topics and
notify about them. This feature is **disabled by default**. You can enable it
and configure other options in the Hermes Management configuration.

 Option                                                      | Description                                                                | Default value 
-------------------------------------------------------------|----------------------------------------------------------------------------|---------------
 detection.inactive-topics.enabled                           | enable inactive topics detection                                           | false         
 detection.inactive-topics.inactivity-threshold              | duration after which a topic is considered inactive and first notified     | 60d           
 detection.inactive-topics.next-notification-threshold       | duration after previous notification after which a topic is notified again | 14d           
 detection.inactive-topics.whitelisted-qualified-topic-names | list of qualified topic names that will not be notified event if inactive  | []            
 detection.inactive-topics.cron                              | cron expression for the detection job                                      | 0 0 8 * * *   
 detection.inactive-topics.notifications-history-limit       | how many notification timestamps will be kept in history                   | 5             

The detection job runs on a single instance of Hermes Management that is a
leader based on the leader election Zookeeper instance.

 Option                             | Description                                                                 | Default Value 
------------------------------------|-----------------------------------------------------------------------------|---------------
 management.leadership.zookeeper-dc | Specifies the datacenter of the Zookeeper instance used for leader election | dc            

To make notifying work, you need to provide an implementation of
`pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsNotifier`
