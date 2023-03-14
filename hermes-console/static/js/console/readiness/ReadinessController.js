var readiness = angular.module('hermes.readiness', ['hermes.readiness.repository']);

readiness.controller('ReadinessController', ['$scope', 'ReadinessRepository', 'ConfirmationModal',
  function ($scope, readinessRepository, confirmationModal) {

    $scope.error = null;

    function loadDatacenters() {
      return readinessRepository.getDatacenters().then(function (data) {
        $scope.datacenters = data;
      }).catch(function (e) {
        displayError(e);
      });
    }

    $scope.openModal = function openModal(datacenterInfo) {
      var action = datacenterInfo.status === 'READY' ? "Turn off" : "Turn on";
      confirmationModal.open({
        actionSubject: 'Are you sure you want to ' + action.toLowerCase() + ' the ' + datacenterInfo.datacenter + ' datacenter ?',
        action: action
      }).result.then(function () {
        readinessRepository.setReadiness(datacenterInfo)
          .then(function () {
            clearError();
            return loadDatacenters();
          })
          .catch(function (e) {
            displayError(e);
          });
      });
    };

    function displayError(msg) {
      $scope.error = msg;
    }

    function clearError() {
      $scope.error = null;
    }

    loadDatacenters();
  }]);