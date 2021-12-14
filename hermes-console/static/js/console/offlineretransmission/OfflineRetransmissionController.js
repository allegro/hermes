var offlineRetransmission = angular.module('hermes.offlineRetransmission', [
  'hermes.offlineRetransmission.repository'
]);

offlineRetransmission.controller('OfflineRetransmissionController', ['$scope', 'RetransmissionRepository', 'toaster', '$uibModalInstance', 'topic',
  function ($scope, retransmissionRepository, toaster, $modal, topic) {
    $scope.calendarDaysBack = 1000;
    $scope.retransmissionRequest = {
      targetTopic: null,
      startTimestamp: null,
      endTimestamp: null
    };

    $scope.createTask = function () {
      const request = {
        sourceTopic: topic.name,
        targetTopic: $scope.retransmissionRequest.targetTopic,
        startTimestamp: new Date($scope.retransmissionRequest.startTimestamp).toISOString(),
        endTimestamp: new Date($scope.retransmissionRequest.endTimestamp).toISOString()
      };
      retransmissionRepository.createTask(request)
        .then(function () {
            toaster.pop('success', 'Success', 'Retransmission has been scheduled.');
            $modal.close();
          }
        ).catch(function (response) {
        toaster.pop('error', 'Error ' + response.status, response.data.message);
      });
    };
  }]);
