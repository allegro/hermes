const { defineConfig } = require('cypress')

module.exports = defineConfig({
  e2e: {
    baseUrl: 'http://localhost:8090',
    reporter: "cypress-multi-reporters",
    reporterOptions: {
      configFile: "reporter-config.json"
    }
  }
})
