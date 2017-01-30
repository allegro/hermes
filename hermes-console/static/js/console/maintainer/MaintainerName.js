angular.module('hermes.maintainer')
    .controller('nameCtrl', ['$scope', 'MaintainerService', function ($scope, maintainerService) {
        $scope.$watch('maintainer', function (newMaintainer) {
            if (newMaintainer === undefined) {
                $scope.name = "";
            } else {
                maintainerService.getMaintainer(newMaintainer.source, newMaintainer.id).then(function (maintainer) {
                    $scope.name = maintainer.name;
                });
            }
        });
    }])
    .directive('maintainerName', function () {
        return {
            controller: 'nameCtrl',
            restrict: 'E',
            template: '{{name}}',
            scope: {
                maintainer: '='
            }
        };
    });