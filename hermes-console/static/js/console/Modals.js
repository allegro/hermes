var modals = angular.module('hermes.modals', []);

modals.factory('ConfirmationModal', ['$uibModal', function ($modal) {
        return {
            open: function (actionDescription) {
                return $modal.open({
                    templateUrl: 'partials/modal/confirm.html',
                    controller: 'ConfirmationController',
                    size: 'lg',
                    resolve: {
                        actionDescription: function () {
                            return actionDescription;
                        }
                    }
                });
            }
        };
    }]);

modals.controller('ConfirmationController', ['$scope', 'actionDescription',
    function ($scope, actionDescription) {
        $scope.description = actionDescription;
    }]);
