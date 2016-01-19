# Data model

This chapter describes domain language of Hermes, that is used consistently throughout all documentation. It maps
directly to the way Hermes stores metadata and how users interact with the system.

## Main concepts

* **publisher** sends messages to Hermes
* **subscriber** expects to receive messages from Hermes
* **group** is a group of topics administered by one publisher, for example to divide whole topics space into
    domain and bounded context groups
* **topic** holds messages of same type, defines type, schema and persistence of all messages stored,
    subscribers can subscribe to messages stored on topics
* **subscription** is created per topic, holds information about consumed messages and other subscriber defined
    properties like maximum delivery rate or retry policy

## Naming convention

**Topic** is usually referred using full-qualified name, which consists of group name and topic name separated by dot.
**Group name** can contain any characters - letters., numbers, dots. However **topic name** can not contain dots.

* group name: `tech.allegro.hermes`
* topic name: `eventsPublished`
* full-qualifed topic name: `tech.allegro.hermes.eventsPublished`

In full-qualified topic name, topic name is always the segment after the last dot.

**Subscriptions** names are used only in context of topic, thus there are no special conventions for their names. They
only have to be unique locally, in scope of current topic.

## Interaction

**Publisher** creates **group** that will hold **topics**. When **topic** is configured, **publisher** can start
publishing messages. **Subscriber** creates **subscription** on given topic to receive published messages. **Subscriber**
receives messages starting from moment of subscription creation.
