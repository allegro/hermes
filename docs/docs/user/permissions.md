# Ownership and permissions

Enforcement of ownership and permission rules is deployment specific, so in addition to reading these general rules,
ask your Hermes administrator how are they enforced. **By default there is no authorization mechanism in place.**

All GET operations can be done without any form of authorization.

## Ownership

Each **group** and **subscription** in Hermes is owned by a **Support Team**. Hermes is own and administered by an
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
Add new topic         | **group owner**
Remove existing topic | **group owner**
Modify topic          | **group owner**

## Subscriptions

Operation                    | Permissions
---------------------------- | -----------
Add new subscription         | any logged in user
Remove existing subscription | **subscription owner** or **group owner**
Modify subscription          | **subscription owner** or **group owner**
Retransmit messages          | **subscription owner** or **group owner**
