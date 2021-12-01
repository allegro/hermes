var repository = angular.module('hermes.offlineRetransmission.repository', ['hermes.discovery']);

repository.factory('RetransmissionRepository', ['DiscoveryService', '$resource',
  function (discovery, $resource) {
    const retransmissionEndpoint = $resource(discovery.resolve('/offline-retransmission/tasks/'), null, {save: {method: 'POST'}});

    return {
      createTask: function (task) {
        return retransmissionEndpoint.save({}, task).$promise;
      }
    };
  }]);
