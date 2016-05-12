# Custom protocol support

By default Hermes Consumer can serve data using HTTP, HTTPS and JMS (HornetQ) protocols. In case that is not enough,
it is possible to implement support for any custom protocol.

## Creating message sender

The most important bit is creating the `ProtocolMessageSenderProvider` which acts as a *factory* that produces the
instance of `MessageSender`. This should be registered on application startup via `HermesConsumerBuilder`:

```java
builder.withMessageSenderProvider("myProtocol", (serviceLocator) -> {
    new MyProtocolMessageSenderProvider()
});
```

The `HermesConsumerBuilder#withMessageSenderProvider` method accepts a lazy evaluated function which gives access to
containers `ServiceLocator`. In some more advanced cases it might be beneficial to use some internal components,
however there are no compatibility guarantees.

### Extending HTTP message sender

In case you would like to build on existing implementation of HTTP messages sender, it can be created and registered
to support other protocols. This is commonly used to add some pseudo-protocols support.

This example shows how to implement the `service://` pseudo protocol. We use it internally to integrate with Service
Discovery: `service://my-service` means that address of the endpoint should be resolved by querying Service Discovery
for instances of `my-service` service.

To achieve this, implement `EndpointAddressResolver` interface and inject the implementation into the new instance of
`JettyHttpMessageSenderProvider`:

```java
builder.withMessageSenderProvider("http", (s) ->
                        new JettyHttpMessageSenderProvider(
                            s.getService(HttpClient.class),
                            s.getService(ConfigFactory.class),
                            myCustomResolver,
                            new DefaultHttpMetadataAppender())
                        )
```

## Management support

Hermes Management validates user input, including protocols used. For management to recognize custom protocols,
specify all custom protocols in the configuration:

```yaml
subscription:
  additionalEndpointProtocols: service, myProtocol
```
