// Karma configuration
// Generated on Wed Oct 15 2014 23:49:21 GMT+0200 (CEST)

module.exports = function(config) {
  config.set({

   plugins:[
             'karma-jasmine',
             'karma-requirejs',
             'karma-coverage',
             'karma-junit-reporter',
             'karma-chrome-launcher',
             ],

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',


    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],


    // list of files / patterns to load in the browser
    files: [
      "test/fixture/config.js",
      "static/components/angular/angular.js",
      "static/components/angular-ui-router/release/angular-ui-router.min.js",
      "static/components/angular-bootstrap/ui-bootstrap-tpls.min.js",
      "static/components/angular-resource/angular-resource.js",
      "static/components/angular-sanitize/angular-sanitize.js",
      "static/components/angular-animate/angular-animate.min.js",
      "static/components/lodash/dist/lodash.min.js",
      "static/components/angularjs-toaster/toaster.js",
      "static/components/json-formatter/dist/json-formatter.min.js.js",
      "static/components/hello/dist/hello.min.js",
      "static/js/**/*.js",
      "node_modules/angular-mocks/angular-mocks.js",
	  "test/unit/**/*.js"
    ],

    junitReporter : {
          outputFile: 'test_out/unit.xml',
          suite: 'unit'
    },

    // list of files to exclude
    exclude: [
        "static/js/bootstrap.js",
    ],


    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
      preprocessors: {
          // source files, that you wanna generate coverage for
          // do not include tests or libraries
          // (these files will be instrumented by Istanbul)
          'js/**/*.js': ['coverage']
      },

      // optionally, configure the reporter
      coverageReporter: {
          type : 'html',
          dir : 'coverage/'
      },


    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['progress', 'junit', 'coverage'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['ChromeHeadless'],


    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true
  });
};
