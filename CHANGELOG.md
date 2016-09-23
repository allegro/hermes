## 0.8.12 (23.09.2016)

### Features

#### ([#566](https://github.com/allegro/hermes/issues/566)) Auditing management operations

All operations in Management can be auditable. By default this option is disabled, but can be enabled using:

```
audit.enabled = true
```

By default changes are sent to logs, but own implementation can be provided. Reed more in [auditing documentation](http://hermes-pubsub.readthedocs.io/en/latest/configuration/security/#management-operations-auditing).

#### ([#481](https://github.com/allegro/hermes/issues/481)) Delay between retries in Hermes Client

It is now possible to specify delay between consecutive retries of sending message.

```java
HermesClient client = HermesClientBuilder.hermesClient(...)
    .withRetries(3)
    .withRetrySleep(100, 10_000)
```

The delay can rise exponentially in specified range (100ms to 10 seconds in example above).

### Bugs

#### ([577](https://github.com/allegro/hermes/issues/557)) Consumer won't stop if there are messages in send queue

#### ([579](https://github.com/allegro/hermes/pull/579)) Wrong path to lag stats in Hermes Console

## 0.8.11 (24.08.2016)

### Features

#### ([#359](https://github.com/allegro/hermes/issues/359)) OAuth2 support [incubating]

Hermes supports Resource Owner Password Credential Grant scenario. It is possible to declare multiple OAuth providers
in Hermes, along with their credentials. Each subscription can choose a provider and defines own user & password.

### Enhancements

#### ([#556](https://github.com/allegro/hermes/pull/556)) Added source and target hostname information to tracking

Tracking information now contains additional fields: `hostname` and `remote_hostname`, which are:

* on Frontend side:
    * `hostname`: hostname of Frontend host that received the message
    * `remote_hostname`: IP address of events producer (who published)
* on Consumers side:
    * `hostname`: hostname of Consumer host that was handling the message
    * `remote_hostname`: IP address/hostname of host that acknowledged/rejected message (who received)

#### ([#561](https://github.com/allegro/hermes/pull/561)) Consumers process model improvements

Improving the stability of new internal Consumers process model by adding consumer process graceful shutdown
and filtering unwatned signals (i.e. sequential START & STOP) which might cause instability.

For monitoring purposes two new metrics (counters) were created in Consumers that compare the assignments state vs 
the actual consumers running:

* `consumers-workload.monitor.missing.count` - how many processes are missing compared to assigned amount
* `consumers-workload.monitor.oversubscribed.count` - how many processes exist although they should not, as this
    instance of Consumers is not assigned to run them

In addition to metrics, warning logs are emitted with details about subscription names missing/oversubscribed.

#### ([#563](https://github.com/allegro/hermes/pull/563)) Apache Curator 2.11.0 and Guava 19.0
