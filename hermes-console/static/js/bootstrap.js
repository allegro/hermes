deferredBootstrapper.bootstrap({
  element: document,
  module: 'hermes',
  resolve: {
    HERMES_URLS: ['$http', '$q', function ($http, $q) {
        var serviceDiscovery;

        if(config.hermes.discovery.type === 'consul') {
            serviceDiscovery = new ConsulServiceDiscovery(config.hermes.discovery.consul, $http, $q);
        }
        else {
            serviceDiscovery = new SimpleServiceDiscovery(config.hermes.discovery.simple.url, $q);
        }
        return serviceDiscovery.resolveInstances();
    }]
  }
});

function removeTrailingSlash(url) {
    if (url.indexOf("/", url.length - 1) !== -1) {
        return url.slice(0, -1);
    }
    return url;
}

function SimpleServiceDiscovery(url, $q) {
    var normalizedUrl = removeTrailingSlash(url);

    this.resolveInstances = function() {
        return $q(function(resolve) { resolve([normalizedUrl]); });
    };
}

function ConsulServiceDiscovery(config, $http, $q) {
    var serviceName = config.serviceName;

    this.resolveInstances = function() {
        return $http.get(config.agentUrl + '/v1/catalog/datacenters')
        .then(function(response) {
            var datacenters = response.data;
            var promises = [];
            for(var i = 0; i < datacenters.length; ++i) {
                promises.push($http.get(config.agentUrl + '/v1/catalog/service/' + serviceName + '?dc=' + datacenters[i]));
            }
            return $q.all(promises);
        })
        .then(function(results) {
            var instances = [];
            for(var i = 0; i < results.length; ++i) {
                for(var j =0; j < results[i].data.length; ++j) {
                    instances = instances.concat(createUrl(results[i].data[j]));
                }
            }
            return instances;
        });
    };

    function createUrl(data) {
        return 'http://' + data.ServiceAddress + ':' + data.ServicePort;
    }
}
