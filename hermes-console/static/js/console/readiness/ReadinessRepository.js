var repository = angular.module('hermes.readiness.repository', ['hermes.discovery']);

repository.factory('ReadinessRepository', ['DiscoveryService', '$resource',
  function (discovery, $resource) {
    var queryEndpoint = $resource(discovery.resolve('/readiness/datacenters'), null, {
      query: {
        method: 'GET',
        isArray: true
      }
    });
    var setReadinessEndpoint = $resource(discovery.resolve('/readiness/datacenters/:datacenter'), null, {save: {method: 'POST'}});

    return {
      getDatacenters: function () {
        return queryEndpoint.query().$promise.then(function (data) {
          return data;
        })
      },
      setReadiness: function (datacenterInfo) {
        return setReadinessEndpoint.save({datacenter: datacenterInfo.datacenter}, {isReady: !datacenterInfo.isReady}).$promise;
      }
    };
  }]);
