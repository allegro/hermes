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

## Subscriptions

Operation                    | Permissions
---------------------------- | -----------
Add new subscription         | any logged in user
Remove existing subscription | **subscription owner** or **topic owner**
Modify subscription          | **subscription owner** or **topic owner**
Retransmit messages          | **subscription owner** or **topic owner**
