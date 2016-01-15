Getting started
===============
This guide walks through setting up a working Hermes environment and making an end-to-end flow of publishing and receiving messages.

Requirements
------------

* `Vagrant <https://www.vagrantup.com/>`_ (1.7.3+) with `VirtualBox <https://www.virtualbox.org/>`_ (4.0.x, 4.1.x, 4.2.x, 4.3.x, 5.0.x)
* curl
* some running receiver service (in this guide we'll use online tool http://requestb.in)

Setting up an environment
-------------------------
Hermes is composed of 3 components: frontend, consumers and management, also a running **Kafka** and **Zookeeper** instances are required, so it's a bit tricky to fire it up from scratch.
The simplest way to try Hermes is by running it with **Docker**.

Running the latest release of Hermes with Docker
________________________________________________

Prebuilt `Docker <https://docs.docker.com/engine/installation/#installation>`_ images of latest Hermes distribution are publicly available at `Docker Hub <https://hub.docker.com/r/allegro/>`_.

To run the whole system, including latest Hermes release, Kafka, Zookeeper and Graphite (metrics), we use `Docker Compose <https://docs.docker.com/compose/install/>`_.
We recommend using `Vagrant <https://www.vagrantup.com/>`_ for virtualization purpose as it isolates and simplifies the setup - e.g. neither Docker nor Docker Compose has to be installed on your workstation.

.. code-block:: bash

    cd ~
    mkdir hermes-latest
    cd hermes-latest
    curl -O https://raw.githubusercontent.com/allegro/hermes/master/docker/docker-compose.yml # download the Docker Compose configuration file
    curl -O https://raw.githubusercontent.com/allegro/hermes/master/docker/Vagrantfile # download Vagrant configuration
    vagrant up  # build and run the virtual machine

Still, you can run the Docker Compose setup **without Vagrant**, but this may require changing some properties (like ``KAFKA_ADVERTISED_HOST_NAME`` - `read more <https://github.com/wurstmeister/kafka-docker>`_),
depending on your environment or OS and in some cases may be hard to debug.

In this scenario download only the `docker-compose.yml <https://raw.githubusercontent.com/allegro/hermes/master/docker/docker-compose.yml>`_ file and run:

.. code-block:: bash

    docker-compose -f /path/to/docker-compose.yml up -d

If you want to run the containers in the foreground skip the ``-d`` option. Stop it with ``Ctrl-C`` (foreground) or ``docker-compose stop`` (background).

Running custom Hermes build
___________________________

You can easily run your own Hermes build:

.. code-block:: bash

    cd ~
    git clone https://github.com/allegro/hermes.git # download Hermes sources
    cd hermes
    vagrant up # create and run a virtual machine with Hermes built from source

In this case we don't use Docker containers, but rather run Zookeeper, Kafka and all Hermes jars on one host for simplicity. To manage the processes we use `supervisord <http://supervisord.org/>`_.
You should consider this setup as a development-playground, because Hermes is a :doc:`distributed system by design </contents/overview/01_architecture>` and it's modules should be run and managed separately.

You can study the VM-provisioning scripts at the ``vagrant_provisioning`` directory. This will help you setup Hermes without Vagrant on your specific environment.

Checking the setup
__________________

If the system is running, you should be able to invoke some management endpoint by making a call to Hermes REST API:

.. code-block:: bash

    curl http://10.10.10.10:8090/topics
    []

Creating topic
--------------

Now you're ready to create a **topic** for publishing messages.

In Hermes messages are published on topics which are aggregated into **groups**. So, you'll need to create a group first, let's name it ``com.example.events``:

.. code-block:: bash

    curl -d '{"groupName": "com.example.events"}' -H "Content-Type: application/json" http://10.10.10.10:8090/groups

The group name is specified in ``groupName`` field of provided JSON. We recommend naming a group with the `FQDN pattern <https://en.wikipedia.org/wiki/Fully_qualified_domain_name>`_.

Now we can list groups to ensure it's been created:

.. code-block:: bash

    curl http://10.10.10.10:8090/groups
    ["com.example.events"]

Okay, now it's time to create a topic in our group. Imagine you want to track user clicks, so we name it ``com.example.events.clicks``:

.. code-block:: bash

    curl -d '{"name": "com.example.events.clicks"}' -H "Content-Type: application/json" http://10.10.10.10:8090/topics

And list topics:

.. code-block:: bash

    curl http://10.10.10.10:8090/topics
    ["com.example.events.clicks"]

Voila!

Publishing and receiving messages
---------------------------------

To receive messages that are published on topic you have to create a **subscription**. This is where you tell Hermes where to send messages published on a topic. You can have many subscriptions on a single topic (in particular - none).

So let's create a ``clicks-receiver`` subscription:

.. code-block:: bash

    curl -d '{"name": "clicks-receiver", "endpoint": "http://requestb.in/1isy54g1", "supportTeam": "my-team"}' -H "Content-Type: application/json" http://10.10.10.10:8090/topics/com.example.events.clicks/subscriptions

(replace ``http://requestb.in/1isy54g1`` with your local service url or your own RequestBin link)

List topic subscriptions:

.. code-block:: bash

    curl http://10.10.10.10:8090/topics/com.example.events.clicks/subscriptions
    ["clicks-receiver"]

Now it's time for a grand finale. Let's publish a message on our topic (note that default Hermes publishing port is :code:`8080`):

.. code-block:: bash

    curl -v -d '{"id": 12345, "page": "main"}' http://10.10.10.10:8080/topics/com.example.events.clicks

    < HTTP/1.1 201 Created
    < Hermes-Message-Id: 66feaead-0685-491e-9c87-00f940ead2c9
    < Content-Length: 0
    < Date: Mon, 04 May 2015 02:18:23 GMT

Congratulations! The message should be delivered to your service or visible via e.g. ``http://requestb.in/1isy54g1?inspect``.

Stopping the system
___________________

To stop the virtual machine run:

.. code-block:: bash

    vagrant halt

Run it again with:

.. code-block:: bash

    vagrant up

Destroy the VM with:

.. code-block:: bash

    vagrant destroy
