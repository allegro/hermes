# Subscribing

Hermes uses **push** model to send messages from broker to subscribers. Hermes takes care of retrying, throttling and
all other little details. What subscriber needs to do, is create endpoint that will accept HTTP POST request.
Subscription is always created in context of a topic. Subscriber will receive only messages published after subscription
was created.

## Creating subscription

Use Hermes Management REST API to create subscription by sending POST request with `application/json` content type
on topics `subscriptions` resource:

```
/topics/{topicName}/subscriptions
```

Request body must contain at least:

- topicName : fully qualified name of topic including group name, separated with a dot (see: [naming convention](/overview/data-model#naming-convention))
- name: name of subscription
- description: subscription description
- endpoint: valid URI
- owner: who's the owner of this subscription (refer to [creating topic](/user/publishing/#creating-topic) for more information)

Minimal request:

```json
{
    "topicName": "group.topic",
    "name": "mySubscription",
    "description": "This is my subscription",
    "endpoint": "http://my-service",
    "owner": {
        "source": "Plaintext",
        "id": "My Team"
    }
}
```

All options:

Option                               | Description                                         | Default value
------------------------------------ | ----------------------------------------------------| -------------
trackingMode                         | track outgoing messages                             | trackingOff
subscriptionPolicy.rate              | maximum sending speed in rps (per DC)               | 400
subscriptionPolicy.messageTtl        | inflight Time To Live in seconds                    | 3600
subscriptionPolicy.messageBackoff    | backoff time between retry attempts in millis       | 1000
subscriptionPolicy.retryClientErrors | retry on receiving 4xx status                       | false
subscriptionPolicy.requestTimeout    | request timeout in millis                           | 1000
subscriptionPolicy.socketTimeout     | maximum time of inactivity between two data packets | infinity
subscriptionPolicy.inflightSize      | max number of pending requests                      | 100
subscriptionPolicy.backoffMultiplier | backoff multiplier for calcaulting message backoff  | 1
subscriptionPolicy.backoffMaxIntervalInSec | maximal retry backoff in seconds              | 600
headers                              | additional HTTP request headers                     | [] (array of headers)
filters                              | used for skipping unwanted messages                 | [] (array of filters)
endpointAddressResolverMetadata      | additional address resolver metadata                | {} (map)
subscriptionIdentityHeadersEnabled   | attach HTTP headers with subscription identity      | false

Possible values for **trackingMode** are:

- trackingAll
- discardedOnly
- trackingOff

Request that specifies all available options:

```json
{
    "topicName": "group.topic",
    "name": "mySubscription",
    "endpoint": "http://my-service",
    "description": "This is my subscription",
    "trackingMode": "trackingAll",
    "owner": {
        "source": "Plaintext",
        "id": "My Team"
    },
    "contact": "my-team@my-company.com",
    "subscriptionPolicy": {
        "rate": 100,
        "messageTtl": 3600,
        "retryClientErrors": false,
        "messageBackoff": 100,
        "requestTimeout": 1000,
        "socketTimeout": 500,
        "inflightSize": 100,
        "backoffMultiplier": 1.0,
        "backoffMaxIntervalInSec": 600
    },
    "headers": [
        {"name": "SOME_HEADER", "value": "ABC"},
        {"name": "OTHER_HEADER", "value": "123"}
    ],
    "filters": [
        {"type": "jsonpath", "path": "$.user.name", "matcher": "^abc.*"},
        {"type": "jsonpath", "path": "$.user.status", "matcher": "new"},
        {"type": "jsonpath", "path": "$.user.name", "matcher": "^abc.*", "matchingStrategy": "all"},
        {"type": "jsonpath", "path": "$.user.status", "matcher": "new"},
        {
            "type": "jsonpath",
            "path": "$.addresses.*.country",
            "matcher": "GB",
            "matchingStrategy": "any"
        }
    ],
    "endpointAddressResolverMetadata": {
        "ignoreMessageHeaders": true,
        "serviceInstanceId": 123
    },
    "subscriptionIdentityHeadersEnabled": false
}
```

## Suspending subscription

It is possible to suspend any subscription. This means, that no messages will be sent, but the information about last
consumed message is preserved. After reactivating subscription, sending starts from the point where it stopped.

To change subscription status send POST request with `application/json` content type:

```
/topics/{topicName}/subscriptions/{subscriptionName}/state
```

with new status name in body (quotation marks are important!):

* to **suspend**: `"SUSPENDED"`
* to **activate**: `"ACTIVE"`

## Failure & success

Hermes treats any response with **2xx** status code as successful delivery (e.g. 200 or 201).

Responses with **5xx** status code or any network issues (e.g. connection timeout) are treated as failures, unless it
is **503** (or **429**) code, described in [back pressure section](#back-pressure).

Responses with **4xx** status code are treated as failures (except **429**, see above), but by default they are not retried. 
This is because usually when subscriber responds with *400 Bad Message* it means this message is somehow invalid and will never be parsed,
no matter how many times Hermes would try to deliver it. This behavior can be changed using **retryClientErrors**
flag on subscription.

## Retries

Hermes Consumers have been optimized towards maximizing chances of successful message delivery. Retry policy is
fairly simple for the client to grasp, as it is configured using four parameters: **Inflight Time To Live (TTL)**,
 **Retry backoff**, **Backoff multiplier** and **Maximum backoff**.

**Inflight TTL** is specified in seconds and it limits for how long the message will be kept inflight - read out of
Kafka, but not delivered to subscriber yet. During this period of time Hermes Consumers tries to deliver the message.
In case of failure, next delivery is scheduled after minimum of **retry backoff** time, which is specified in milliseconds.
Message offset will not be committed to broker unless it's retry limit has been exhausted (it has been delivered or discarded).

How many times a message can be retried? Very rough calculation can be made using this formula (applies only for constant
retry backoff):

```
retries = to_millis(inflight_ttl) / retry_backoff
```

This calculation does not take rate limiting into a count, so answer to this question is much more complicated.

We decided to use time-based configuration (as opposed to specifying retry count), because the malfunctions
of subscribing services are time constrained in one way or another. It is much easier to state, that in case of service
failure the rescue team has one hour to fix the problem before any event will be discarded.

By default inflight TTL is set to 3600 seconds (an hour) and retry backoff is set to 100ms.
We set a hard limit for the inflight TTL to 7200 seconds (two hours).


### Constant and exponential retry backoff

Retry backoff is calculated using the following formula:

```
current_backoff = previous_backoff * backoff_multiplier
```
This has the following consequences:

Backoff multiplier               | Retry policy type                                      
---------------------------------|--------------------------------
1                                | Constant retry backoff
  above 1                        | Exponential retry backoff
  
  
The hard limit to current backoff is defined by maximum backoff parameter and by default is equal to 600 s. 

It is worth mentioning that the calculation of current backoff is ignored when the  **Retry-After** header is used.                    

### Retries counter

Each message sent by Hermes using HTTP sender comes with an additional header: **Hermes-Retry-Count**. It contains a number
of retries for this specific message done by this Consumer instance.

The number of retries is counted locally, meaning it can not be treated as a global counter of delivery attempts.
This counter will reset when:

* Consumer instance is shut down before committing the offset to Kafka and other instance attempts to deliver this
    message
* messages are retransmitted

## Back pressure

The client is able to signal it can't handle the message at the moment and Hermes Consumer will retry delivery after
minimum of given delay.

The endpoint can return **Retry-After** header, with the amount of seconds to backoff, combined with status **429** or **503**. This
is the only case when returning **4xx** or **5xx** code does not result in slowing down the overall [sending speed](#rate-limiting).

Regardless of the provided delay, the **Inflight TTL** of the message still applies in this situation,
therefore the endpoint needs to ensure the total delay of consecutive **Retry-After** responses does not exceed this value.
In case it does, the message is discarded.

An important limitation to remember is that the offset won't be committed until the message is either
successfully delivered or discarded and in case of consumer failure all messages following (even when successfully processed)
will be resent.

## Rate limiting

Each subscription can define a hard limit of accepted messages per second and Hermes will never cross this line. However
below this treshold, rate limiting algorithm tries to match sending speed with current capabilities of subscriber.

For example lets take subscriber A who has declared that he is able to receive 100 msg/sec at maximum. Hermes will be
sending messages at this rate. Now assume that there is a problem with subscriber A - 10% of requests gets timed out.
Hermes will lower the speed up to the moment, when it sees no dropped requests. When subscriber A has dealt with problems,
the speed will automatically increase to reach the maximum.

This is important when trying to understand why subscriber receives less messages than expected or the subscribers lag
is growing. First things first, you should check subscription metrics for signs of any problems.

If you want to know the exact algorithm, check [rate limiting configuration page](/configuration/rate-limiting/).

## Additional headers

Each subscription can define a number of additional `headers` that will be added to every HTTP request when sending messages.
They can be useful on test environments to pass security tokens or on production to communicate with some legacy systems that
require custom headers.

## Endpoint address resolver metadata

Custom implementation of Consumer's `EndpointAddressResolver` interface can make use of provided `endpointAddressResolverMetadata`
for selecting address resolving strategies. This object is deserialized to `Map<String, Object>` and may contain any data needed,
like feature flags.

It's ignored by the default implementation.

See [console integration](/configuration/console/#subscription-configuration) for more information.

## Message filtering

Each subscription can define set of filters that are going to be applied after receiving message from kafka in order
of their declaration.

### Choosing appropriate filter

This mainly concerns message content type. Filtering is done *before* any conversion takes place so all messages have
the same content type as topic on which they were published.

Topic content-type    | Filter type
--------------------- | -----------
avro                  | avropath
json                  | jsonpath

### Matching strategy

Filter path can be described in a way that it indicates several fields,
e.g. a `*` sign in JsonPath or array indicator in AvroPath.
By default all fields *must* match the matcher, but this behaviour can be changed
with `matchingStrategy`. Possible values are:

* `"all"` (default)
* `"any"`

Example:
```
{
    "type": "jsonpath",
    "path": "$.user.addresses.*.country",
    "matcher": "GB",
    "matchingStrategy": "all"
}
```

This filter will pass the message only when in all user addresses country will match *GB*.
In case when `matchingStrategy` would be set to `any` then all messages with *GB* country set in any address will be passed.

### JsonPath configuration

JsonPath filter is based on popular [library](https://github.com/jayway/JsonPath) of the same name that can query
json documents. In this case it is used as a selector to retrieve value that is later matched by regexp.

Option                | Description
--------------------- | ---------------------------------------------------
type                  | type of filter
path                  | JsonPath expression to query json document
matcher               | regexp expression to match value from json document
matchingStrategy      | type of matching strategy. Default is `all`

Example:
```
{"type": "jsonpath", "path": "$.user.name", "matcher": "^abc.*", "matchingStrategy": "all"}
```

### AvroPath configuration

AvroPath is our custom filter that works with avro documents. Currently there are no commonly used query languages for
avro so we decided to introduce very simple dotted path format without any advanced features. It is very easy to
understand if you're familiar with JsonPath. Right now array and basic selectors that point to specific fields are
supported.

Option                | Description
--------------------- | ---------------------------------------------------
type                  | type of filter
path                  | dotted expression to query avro document. When array selector is used then wildcard sign `*` can be used as index
matcher               | regexp expression to match value from avro document
matchingStrategy      | type of matching strategy. Default is `all`

Example:
```
{"type": "avropath", "path": ".user.name", "matcher": "^abc.*"}
{"type": "avropath", "path": ".user.addresses[1].city", "matcher": "^abc.*"}
{"type": "avropath", "path": ".user.addresses[*].city", "matcher": "^abc.*"}
{
    "type": "avropath",
    "path": ".user.addresses[*].city",
    "matcher": "^abc.*",
    "matchingStrategy": "any"
}
```

### Adding filters

We support editing filters via UI. Click edit subscription and add or remove a particular filter.
Also filters can be added during creation of a new subscription.

And it can be done by api also. Send PUT request for subscriptions endpoint.
Example:
```
curl  -H "Content-Type: application/json" -X PUT "http://{hermesManagementUrl}/topics/{topicName}/subscriptions/{subscriptionName}" -d '{"filters": [{"type": "avropath", "path": ".user.name", "matcher": "^abc.*"}]}'
```

## Authorization

### Basic Auth

Subscriber can authorize Hermes using [Basic Auth](https://en.wikipedia.org/wiki/Basic_access_authentication). To enable
Basic Auth for a subscription, pass credentials in endpoint definition, for example:

```
http://user:password@example.com
```

Password is never displayed in public and is not available via API. When editing subscription endpoint, remember that
you need to provide full credentials.

### OAuth

Hermes supports OAuth 2 [resource owner password](https://tools.ietf.org/html/rfc6749#section-4.3)
and [client credentials](https://tools.ietf.org/html/rfc6749#section-4.4) grants for subscription endpoints authorization.

To enable OAuth, first register Hermes as an OAuth client in your OAuth provider service.
Hermes will be given it's unique `id` and `secret`.

#### Registering an OAuth provider

The Hermes administrator needs to define an OAuth provider authority that is responsible
for issuing OAuth tokens for subscriptions. There can be many OAuth providers configured in Hermes.
A single OAuth provider registration can be configured for a given subscription.

To register an OAuth provider in Hermes send `POST` request with to `/oauth/providers` of Hermes-management:

```json
{
    "name": "myOAuthService",
    "tokenEndpoint": "https://oauth.example.com/oauth2/token",
    "clientId": "myHermes",
    "clientSecret": "abc123",
    "tokenRequestInitialDelay": 1000,
    "tokenRequestMaxDelay": 30000,
    "requestTimeout": 1000,
    "socketTimeout": 500
}
```

Field                    | Description
------------------------ | ---------------------------------------------------
name                     | Hermes-wide id of a specific OAuth provider
tokenEndpoint            | Token request URL of the provider
clientId                 | OAuth client id of Hermes
clientSecret             | OAuth client secret of Hermes
tokenRequestInitialDelay | Min delay between possible token requests
tokenRequestMaxDelay     | Max delay between possible token requests
requestTimeout           | HTTP timeout for token request
socketTimeout            | Maximum time of inactivity between two data packets

Verify the OAuth provider is registered by calling `GET` on `/oauth/providers` and `/oauth/providers/{providerName}` endpoints.
Hermes HTTP endpoints return asterisks (`******`) in place of the actual secrets.

**Important**: Note that OAuth configuration credentials (secrets, passwords) are stored as plaintext in Zookeeper.
Make sure access to it is [properly secured](/configuration/kafka-and-zookeeper#Zookeeper)!

#### Requesting tokens

When Hermes tries to send a message to an OAuth-secured subscription and it gets `401 Unauthorized` response,
it will request an OAuth token using the configured OAuth policy's credentials ([see below](#securing-subscription)).
The message will be resent to subscriber along with the issued token (`Authorization: Bearer <token>` header).
Hermes will resend messages to OAuth-secured subscribers irrespectively from `retryClientErrors` subscription setting value.

To prevent from requesting tokens too often (when subscription is responding with 401 for some unknown reason
even though token is provided) Hermes will rate limit it's token requests using `tokenRequestInitialDelay` and `tokenRequestMaxDelay`
values set for subscription's OAuth provider. The delay duration grows exponentially and is being reset to initial value
after each `200 OK` response (meaning the token is valid and there's no need to request a new one).

The tokens are stored in-memory and are not distributed between Hermes consumer nodes meaning each node requests
it's own tokens and performs the token request rate limiting calculation locally.

#### Securing subscription

Both OAuth 2 server-side grants are supported by Hermes in order to secure subscription endpoints.

#### Client credentials grant

[Cient credentials grant](https://tools.ietf.org/html/rfc6749#section-4.4) is the simpler OAuth grant type where a client (Hermes)
is given permission to send messages to subscription endpoint.
To acquire an access token Hermes will use it's credentials configured in a specific OAuth provider definition.

Enable this grant type by extending the subscription definition with `oAuthPolicy` entry, for example:

```json
"oAuthPolicy": {
  "grantType": "clientCredentials",
  "providerName": "myOAuthService",
  "scope": "someScope"
}
```

Field        | Description
-------------| ----------------------------------------------------------
grantType    | Needs to be set to `clientCredentials` for this grant type
providerName | OAuth provider to be used for token request
scope        | An optional scope of the access request

#### Resource owner password grant

[Resource owner password grant](https://tools.ietf.org/html/rfc6749#section-4.3) is a more complex grant type that may be useful
when subscriptions are owned by different users. A subscription endpoint is a resource and the owner wants to be the only
one able to access it. The user needs to provide it's credentials (username and password) to access the resource
and Hermes will request an access token on behalf of the user using these credentials.

**Important**: Note that the current implementation of this grant type performs a single request to the OAuth provider
when requesting token (containing both client's and resource owner's credentials) and the OAuth provider
should be aware of that and support it.

Enable this grant type by extending the subscription definition with following content:

```json
"oAuthPolicy": {
  "grantType": "password",
  "providerName": "myOAuthService",
  "username": "someUser",
  "password": "password123",
  "scope": "someScope"
}
```

Field        | Description
-------------| ----------------------------------------------------------
grantType    | Needs to be set to `password` for this grant type
providerName | OAuth provider name to be used for token request
username     | Resource owner's username
password     | Resource owner's password
scope        | An optional scope of the access request

## Metrics

Subscription metrics are available at:

```
/topics/{topicName}/subscriptions/{subscriptionName}/metrics
```

and include:

* **delivered**: number of delivered messages through the lifetime of subscriptions
* **discarded**: number of delivered messages through the lifetime of subscriptions
* **inflight**: number of messages currently in send Buffer
* **timeouts**: number of requests per second that end in subscribing service timeout
* **otherErrors**: number of requests per second that end in some other network error
* **codes2xx**: number of requests per second that end in returning 2xx HTTP response
* **codes4xx**: number of requests per second that end in returning 4xx HTTP response
* **codes5xx**: number of requests per second that end in returning 5xx HTTP response
* **rate**: current sending rate

## Last undelivered message

It is possible to easily retrieve contents of last undelivered message, along with timestamp and reason why it could
not be delivered.

```
/topics/{topicName}/subscriptions/{subscriptionName}/undelivered/last
```

It will return **404 Not Found** if there is no message to display. Otherwise it information in following format:

```json
{
    "timestamp": 1452952981548,
    "subscription": "subscription-name",
    "topicName": "group.topic-name",
    "status": "DISCARDED",
    "reason": "Total timeout elapsed",
    "message": "body of a message",
    "partition": 5,
    "offset": 368741824,
    "cluster": "primary"
}
```

Partition, offset and cluster specify the position of this message in Kafka, in case it was needed to retrieve it or
to start the retransmission.

## Retransmission

Hermes gives an option to easily retransmit messages that are still available on Kafka. Simply send a PUT to:

```
/topics/{topicName}/subscriptions/{subscriptionName}/retransmission
```

The message body can be of following format:

* ISO date: `2015-09-03T13:30:00.000`
* number of hours to retransmit: `-6h`

Hermes will find message offset in Kafka, that is closes to the given date and initiate retransmission. In return, you will
receive list of offsets from which retransmission will be started per partition.

## Message tracking

When message tracking is enabled on subscription, it is possible to display message flow through Hermes by using its
**Hermes-Message-Id**. Endpoint:

```
/topics/{topicName}/subscriptions/{subscriptionName}/events/{Hermes-Message-Id}/trace
```

returns list of message status changes as it flew through Hermes.

```json
[
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332961,
        "topicName": "topic-name",
        "status": "INFLIGHT",
        "cluster": "cluster"
    },
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332967,
        "topicName": "topic-name",
        "status": "SUCCESS",
        "cluster": "cluster"
    },
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332967,
        "subscription": "subscription-name",
        "topicName": "topic-name",
        "status": "INFLIGHT",
        "partition": 5,
        "offset": 171165098,
        "cluster": "cluster"
    },
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332979,
        "subscription": "subscription-name",
        "topicName": "topic-name",
        "status": "FAILED",
        "reason": "Total timeout elapsed",
        "partition": 5,
        "offset": 171165098,
        "cluster": "cluster"
    },
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332979,
        "subscription": "subscription-name",
        "topicName": "topic-name",
        "status": "SUCCESS",
        "partition": 5,
        "offset": 171165098,
        "cluster": "cluster"
    }
]
```

Depending on the progress, different kind of information is gathered. In the example above, first two traces were written
by **Frontend**. Trace from Frontend contains only timestamp and message id. Last three traces originated in **Consumers**,
and contain additional information, such as delivery status, subscription which received the message and exact position
in Kafka. Consumers trace contains information about each attempt to deliver the message.

Possible statuses for **Frontend** traces are:

* **INFLIGHT**: message has been received, but not acknowledged by Kafka yet, it waits in the buffer
* **SUCCESS**: message has been received and acknowledged by Kafka
* **FAILED**: i don't think we ever seen this status on production

Possible statuses for **Consumers** traces are:

* **INFLIGHT**: message has been read from Kafka and is in sender queue
* **SUCCESS**: message has been successfully delivered
* **FAILED**: this attempt of sending the message failed, it will be retired
* **DISCARDED**: message delivery failed

### Last 100 undelivered messages

With message tracking enabled, it is also possible to list last 100 undelivered messages, as opposed to only last one
without message tracking. This information is available at:

```
/topics/{topicName}/subscriptions/{subscriptionName}/undelivered
```

It returns array of message tracking information in following format:

```json
[
    {
        "messageId": "1d6b9496-5af7-4a06-a27f-df7a6a5719c6",
        "timestamp": 1452955332980,
        "subscription": "subscription-name",
        "topicName": "topic-name",
        "status": "DISCARDED",
        "reason": "Message sending failed with status code:400",
        "cluster": "primary",
        "offset": 171165098,
        "partition": 5
    }
]
```

## Sending delay

Sending delay can be defined for each serial subscription. Consumers will wait for a given time before trying to deliver a message.
This might be useful in situations when there are multiple topics that sends events in the same time, but you want to increase
chance that events from one topics will be delivered later than events from another topic.
