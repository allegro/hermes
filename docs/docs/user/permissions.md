# Ownership and permissions

Enforcement of ownership and permission rules is deployment specific, so in addition to reading these general rules,
ask your Hermes administrator how are they enforced. **By default there is no authorization mechanism in place.**

All GET operations can be done without any form of authorization.

## Ownership

Each **topic** and **subscription** in Hermes is owned by an **owner**. Hermes is owned and administered by an
**administrator**.

## Group

Operation             | Permissions
--------------------- | -----------
Add new group         | **administrator**
Remove existing group | **administrator**
Modify group          | **administrator**

## Topics

Operation             | Permissions
--------------------- | -----------
Add new topic         | any logged in user
Remove existing topic | **topic owner**
Modify topic          | **topic owner**

### Publishing permission

You can configure which services can publish on which topic configuring **topic.auth** section.
How publisher name is evaluated is deployment specific. It can be extracted from ssl certificate or read from supplied header for instance.
Worth noting is that for authorization features to work those have to be enabled on your hermes cluster by your administrator.


Option                       | Description                                         | Options     | Default value
---------------------------- | --------------------------------------------------- | ----------- | -------------
enabled                      | enable topic authorization                          | true, false | false
unauthenticatedAccessEnabled | allow publishing for services without credentials   | true, false | false
publishers                   | array of service names that are allowed to publish  | -           | []

Example:

```json
{
    "name": "my-group.my-topic",
    "description": "This is my topic",
    "contentType": "JSON",
    "retentionTime": {
        "duration": 1
    },
    "owner": {
        "source": "Plaintext",
        "id": "My Team"
    },
    "auth": {
      "enabled": true,
      "unauthenticatedAccessEnabled": false,
      "publishers": ["my-publisher-1", "my-publisher-2"]
    }
}
```

## Subscriptions

Operation                    | Permissions
---------------------------- | -----------
Add new subscription         | any logged in user
Remove existing subscription | **subscription owner** or **topic owner**
Modify subscription          | **subscription owner** or **topic owner**
Retransmit messages          | **subscription owner** or **topic owner**