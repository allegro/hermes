module.exports = function(grunt) {

  grunt.initConfig({
    jshint: {
      files: ['Gruntfile.js', 'static/js/**/*.js', 'test/**/*.js'],
      options: {
        'esnext': 6,
      }    },
    watch: {
      files: ['<%= jshint.files %>'],
      tasks: ['jshint', 'karma']
    },
    karma: {
      unit: {
        configFile: 'karma.config.js',
        browsers: ['ChromeHeadless']
      }
    }
  });

  
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-karma');
  grunt.loadNpmTasks('grunt-contrib-watch');

  grunt.registerTask('test', ['jshint', 'karma']);

  grunt.registerTask('default', ['test']);

};
