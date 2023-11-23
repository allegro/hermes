# Hermes Console

Hermes Console is served by Hermes Management and can be configured via 
[application property files](https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config-application-property-files)
under `console` property.

If you want to see console default config then take a look on
[application-local.yaml](https://github.com/allegro/hermes/blob/master/hermes-management/src/main/resources/application-local.yaml) file.

Option            | Description
----------------- | ---------------------------------------------------------
console.title     | what to display in left upper corner, next to Hermes logo
dashboard.metrics | link to metrics dashboard, available on Console home page
dashboard.docs    | link to documentation, available on Console home page

## Metric Store integration

Hermes console could have a button on the topics and subscriptions view that takes you to a dashboard with metrics.
In order to make it work you have to provide an implementation of `pl.allegro.tech.hermes.management.domain.MetricsDashboardUrlService`.

 Option                              | Description                                                                      
-------------------------------------|--------------------------------------------------------------------------------------
 metrics.fetchingDashboardUrlEnabled | enable fetching dashboard url from hermes-management and show the referring UI button 

## Authorization

Hermes Console supports two types of authorization, which can be enabled at the same time (OAuth > headers):

* OAuth2, using [hellojs](https://adodson.com/hello.js/) - it allows on logging in and each request going to Management will
    contain the `Authorization` header with the token
* header-based - this is the simple method, if passing tokens/passwords around is okay

**Note** none of above mentioned authorization methods are available in Management out of the box at the moment, you need to
provide the implementation.

Option                   | Description
------------------------ | -------------------------------------
auth.oauth.enabled       | enable OAuth support
auth.oauth.url           | url to OAuth authorization endpoint
auth.oauth.clientId      | OAuth client id
auth.oauth.scope         | assigned scope
auth.headers.enabled     | enable simple authorization support
auth.headers.adminHeader | name of header to bear admin password

## Topic configuration

Option                           | Description
-------------------------------- | ------------------------------------------------------------------------------------
topic.messagePreviewEnabled      | show message preview tab on topic view; enable only if message preview is enabled in Frontend
topic.offlineClientsEnabled      | show offline clients section on topic view
topic.offlineClientsIframeSource | source for an inline iframe with view for offline clients

## Subscription configuration

Option                                       | Description
---------------------------------------------|----------------------------------------------------------------------------------
subscription.endpointAddressResolverMetadata | extend subscription address resolver configuration with specified metadata fields

Example:

```json
"subscription": {
    "endpointAddressResolverMetadata": {
        "exampleEntryEnabled": {
            "title": "Example boolean entry",
            "type": "boolean"
        },
        "exampleTextEntry": {
            "title": "Example text entry",
            "type": "text",
            "placeholder": "You should write something here",
            "hint": "This should help somehow..."
        },
        "exampleSelectEntry": {
            "title": "Example select entry",
            "type": "select",
            "options": {
                "": "<clear>",
                "a": "An option",
                "b": "Another option"
            }
        }
    }
}
```

This will add 3 additional fields to subscription configuration, all optional. 

## Owners configuration

Option        | Description
--------------| --------------------------------------
owner.sources | a list of owner sources customizations

Example:

```json
"owner": {
    "sources": [
        {"name": "Plaintext", "placeholder": "this will display as placeholder in the owner input"}
    ]
}
```
