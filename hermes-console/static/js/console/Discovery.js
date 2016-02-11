var discovery = angular.module('hermes.discovery', []);

discovery.factory('DiscoveryService', ['HERMES_URLS', function(urls) {
        var counter = 0;

        return {
            resolve: function(path) {
                return urls[counter++ % urls.length] + path;
            }
        }
    }]);
