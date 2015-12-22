Getting started
===============
This guide walks through setting up a working Hermes environment and making an end-to-end flow of publishing and receiving messages.

Requirements
------------

* Java 8
* `Docker <https://docs.docker.com/installation/#installation>`_ (1.6+) and `Docker compose <https://docs.docker.com/compose/install/>`_
* curl
* some running receiver service (in this guide we'll use online tool http://requestb.in)

Setting up an environment
-------------------------
Hermes is composed of 3 components: frontend, consumers and management, also a running **Kafka** and **Zookeeper** instances are required, so it's a bit tricky to fire it up from scratch.
The simplest way to try Hermes is by running it with **Docker**.

Running the latest release of Hermes
____________________________________

Prebuilt docker images of latest hermes distribution are publicly available at `Docker Hub <https://registry.hub.docker.com/repos/allegro>`_.

To run the whole system, including latest Hermes release, Kafka and Zookeeper, use the ``run.sh`` script which wraps **Docker compose**:

* Download `docker-compose.yml <https://raw.githubusercontent.com/allegro/hermes/master/docker/docker-compose.yml>`_ configuration from Hermes source code and put into an empty directory
* Run ``docker-compose up -d --no-recreate`` from that directory

A handy one-liner would be:

.. code-block:: bash

    $ mkdir $HOME/hermes && cd $_ && curl -O https://raw.githubusercontent.com/allegro/hermes/master/docker/docker-compose.yml && docker-compose up -d --no-recreate

Running custom Hermes build
___________________________

You can easily build your own docker images:

* Download Hermes source code and go to ``docker`` directory
* Run ``./build.sh`` - this builds the source code and prepares a Docker image for each Hermes component
* Modify ``docker-compose.yml`` in order to run your custom images by changing the ``image`` property of each Hermes component to ``allegro/hermes-<component>:<custom image tag>``
* Run ``./run.sh`` (or ``docker-compose up -d --no-recreate``)

Stopping the system
___________________

To stop the containers simply run the :code:`stop.sh` script (or ``docker-compose stop``).

Checking the setup
__________________

If the system is running, you should be able to invoke some management endpoint by making a call to Hermes REST API:

.. code-block:: bash

    curl http://192.168.59.103:8080/topics
    []

(replace :code:`192.168.59.103` with IP of your Docker host).



Creating topic
--------------
Now you're ready to create a **topic** for publishing messages.

In Hermes messages are published on topics which are aggregated into **groups**. So, you'll need to create a group first, let's name it ``com.example.events``:

.. code-block:: bash

    curl -d '{"groupName": "com.example.events"}' -H "Content-Type: application/json" http://192.168.59.103:8080/groups

The group name is specified in ``groupName`` field of provided JSON. We recommend naming a group with the FQDN pattern.

Now we can list groups to ensure it's been created:

.. code-block:: bash

    curl http://192.168.59.103:8080/groups
    ["com.example.events"]

Okay, now it's time to create a topic in our group. Imagine you want to track user clicks, so we name it ``com.example.events.clicks``:

.. code-block:: bash

    curl -d '{"name": "com.example.events.clicks"}' -H "Content-Type: application/json" http://192.168.59.103:8080/topics

And list topics:

.. code-block:: bash

    curl http://192.168.59.103:8080/topics
    ["com.example.events.clicks"]

Voila!

Publishing and receiving messages
---------------------------------

To receive messages that are published on topic you have to create a **subscription**. This is where you tell Hermes where to send messages published on a topic. You can have many subscriptions on a single topic (in particular - none).

So let's create a ``clicks-receiver`` subscription:

.. code-block:: bash

    curl -d '{"name": "clicks-receiver", "endpoint": "http://requestb.in/1isy54g1", "supportTeam": "my-team"}' -H "Content-Type: application/json" http://192.168.59.103:8080/topics/com.example.events.clicks/subscriptions

(replace ``http://requestb.in/1isy54g1`` with your local service url or your own RequestBin link)

List topic subscriptions:

.. code-block:: bash

    curl http://192.168.59.103:8080/topics/com.example.events.clicks/subscriptions
    ["clicks-receiver"]

Now it's time for a grand finale. Let's publish a message on our topic:

.. code-block:: bash

    curl -v -d '{"id": 12345, "page": "main"}' http://192.168.59.103:8888/topics/com.example.events.clicks

    < HTTP/1.1 201 Created
    < Hermes-Message-Id: 66feaead-0685-491e-9c87-00f940ead2c9
    < Content-Length: 0
    < Date: Mon, 04 May 2015 02:18:23 GMT

(note that default Hermes publishing port is :code:`8888`)

Congratulations! The message should be delivered to your service or visible via e.g. ``http://requestb.in/1isy54g1?inspect``.
