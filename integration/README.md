# End-to-end frontend tests

Hermes has end-to-end frontend tests based on [Cypress](https://www.cypress.io) framework.
Tests are located in `integration/src/integration/e2e`. `CypressTest` run these tests using testcontainers.

During development of these tests you can use more interactive way than `CypressTest` using Cypress GUI:
- Start an environment and hermes-management ([instructions](https://hermes-pubsub.readthedocs.io/en/latest/quickstart/#development))
- Go to `integration/src/integration/e2e` directory
- Install Cypress `npm i`
- Open Cypress `./node_modules/.bin/cypress open`
