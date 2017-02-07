Admin scripts that might come in handy.

# Python scripts

The python scripts are simple to run using virtualenv and pip.

Run the commands to prepare the environment:

```bash
# setup and activate virtualenv
$ virtualenv .
$ . bin/activate

# install the dependencies
$ pip install -r requirements.txt

# if you want to deactivate virtualenv
$ deactivate
```

Now you're ready to launch the scripts

migrate_consumers_0.10.5.py
===

When migrating to version >= **0.10.5** of Hermes Consumers, it will be necessary to migrate the assignments in zookeeper.

This will write a marker `AUTO_ASSIGNED` into each node in assignments structure.

Example:
---

```bash
$ python migrate_consumers_0.10.5.py --help
$ python migrate_consumers_0.10.5.py -z localhost:2181 -p /hermes/consumers-workload/primary/runtime
```
