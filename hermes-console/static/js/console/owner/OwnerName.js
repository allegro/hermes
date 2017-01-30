angular.module('hermes.owner')
    .controller('nameCtrl', ['$scope', 'OwnerRepository', function ($scope, ownerRepository) {
        $scope.$watch('owner', function (newOwner) {
            if (newOwner === undefined) {
                $scope.name = "";
            } else {
                ownerRepository.getOwner(newOwner.source, newOwner.id).then(function (owner) {
                    $scope.name = owner.name;
                });
            }
        }, true);
    }])
    .directive('ownerName', function () {
        return {
            controller: 'nameCtrl',
            restrict: 'E',
            template: '{{name}}',
            scope: {
                owner: '='
            }
        };
    });