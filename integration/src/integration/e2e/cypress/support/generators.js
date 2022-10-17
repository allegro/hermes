const { faker } = require('@faker-js/faker');

const generators = {
    group: {
        valid() {
            return `${faker.word.adjective()}.${faker.word.noun()}`
        }
    }
}

module.exports = { generators }
