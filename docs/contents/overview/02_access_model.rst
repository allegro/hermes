Access model
============

Hermes persists information about groups, topics & subscriptions (*metadata*) in Zookeeper in the following hierarchy of nodes::

    /hermes/groups/{groupName}/topics/{topicName}/subscriptions/{subscriptionName}

Topics are aggregated into groups
---------------------------------

Groups can be interpreted as domains or bounded-contexts. Only user with ADMIN role can create them. Users with
GROUP_OWNER role can modify groups, which belong to them.

Groups were introduced in Hermes because we wanted that only users/teams which are responsible for a specific domain have permissions to make modification in it.

When a topic is created then *fully qualified topic name* must be presented. For example request::

    POST http://hermes-management:8080/topics
    {
        "name": "com.example.transaction.order.OrderCreated"
    }

creates topic with name 'OrderCreated' in group 'com.example.transaction.order'.
*Fully qualified topic name* is often used in REST API. It's naming convention is analogous to packages and classes names from Java.

Subscriptions are created for topics
------------------------------------

When a service would like to consume messages from a topic then subscription needed to be created. Users with
ADMIN, GROUP_OWNER and SUBSCRIPTION_OWNER roles can create a subscription.

Roles management
----------------

By default there are no access restrictions placed in management module. However, read
:doc:`/contents/tech/03_deploy_management` for info on how to plug in own authentication/authorization filters.
