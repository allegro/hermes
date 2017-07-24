# Hermes Console

Hermes Console is configured using JSON file, which has to be present in order to start the Console. Its location can be
specified using `-p` command line argument or via `HERMES_CONSOLE_CONFIG` environment variable. Hermes Console can read
config from local filesystem or via HTTP:

```
./run.sh -c /etc/hermes-console/config.json
./run.sh -c http://configuration.store/hermes-console
```

Hermes Console directory contains example configuration file along with some comments (remember to remove them when
copying example - it's not a valid JSON file).

Option            | Description
----------------- | ---------------------------------------------------------
console.title     | what to display in left upper corner, next to Hermes logo
dashboard.metrics | link to metrics dashboard, available on Console home page
dashboard.docs    | link to documentation, available on Console home page

## Hermes Management discovery

Hermes Console can discovery Hermes Management using two approaches:

* `simple`: provide direct URL
* `consul`: use [Consul](http://consul.io) service discovery to discover instances

In case of using Consul, implementation is very naive: it fetches list of registered hosts only on page load, thus if
anything changes during user session it is necessary to refresh the page.

Option                              | Description
----------------------------------- | ----------------------------------------------------------
hermes.discovery.type               | discovery mechanism to use (`simple` or `consul`)
hermes.discovery.simple.url         | direct address to Hermes Management
hermes.discovery.consul.agentUrl    | address to Consul Agent
hermes.discovery.consul.serviceName | name under which Hermes Management is registered in Consul


## Metric Store integration

Hermes Console can be integrated with Metric Store. This means, that metrics shown in Console can link to actual graphs
plotted by Metric Store. At the moment only Graphite is supported.

Option                  | Description
----------------------- | ---------------------------------------------------------------------------
metrics.type            | type of metrics storage to link to (currently only `graphite` is supported)
metrics.graphite.url    | URL to graphite
metrics.graphite.prefix | prefix to graphite metrics

## Authorization

Hermes Console supports two types of authorization, which can be enabled at the same time (OAuth > headers):

* OAuth2, using [hellojs](https://adodson.com/hello.js/) - it allows on logging in and each request going to Management will
    contain the `Authorization` header with the token
* header-based - this is the simple method, if passing tokens/passwords around is okay

**Note** none of above mentioned authorization methods are available in Management out of box at the moment, you need to
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

Option                      | Description
--------------------------- | ------------------------------------------------------------------------------------
topic.messagePreviewEnabled | show message preview tab on topic view; enable only if message preview is enabled in Frontend
topic.offlineClientsEnabled | show offline clients section on topic view; enable only if offline readers integration in management module is turned on

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
