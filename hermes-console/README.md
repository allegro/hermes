Hermes Console
====

Hermes admin console - UI for Hermes Management API.

## Build and install

Install backend and frontend dependencies:

```bash
npm install
bower install
```

## Configuration

Hermes Console has two initial configuration options, which can be specified using either
command line or environment variables:

* `-p` or `HERMES_CONSOLE_PORT`: specify port (default: `8000`)
* `-c` or `HERMES_CONSOLE_CONFIG`: specify configuration source: local file or http resource (default: `./config.json`)

For example to run Hermes Console at port 8001 and fetch configuration from remote source:

```bash
node serve.js -p 8001 -c http://configuration-source
```

Other configuration can be found in `config.json.example` file.

## Run

Use provided node script:

```
./serve.js
```

Or explicitly via node:

```
node serve.js
```

For development purposes to have hermes console autoreload changed files it can be run with `nodemon`

Install:
```
npm install nodemon -g
```
Run:
```
nodemon serve.js
```

## Tests

In order to run all tests and `jshint` code analysis run `grunt` tasks:

```
grunt test
```
