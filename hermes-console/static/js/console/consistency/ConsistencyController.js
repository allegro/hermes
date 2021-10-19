var consistency = angular.module('hermes.consistency', [
    'ui.bootstrap',
    'hermes.consistency.repository'
]);

consistency.controller('ConsistencyController', ['$scope', '$state', 'toaster', 'CONSISTENCY_CONFIG', 'ConsistencyRepository',
    function ($scope, $state, toaster, config, consistencyRepository) {

        const consistencyCheckingStates = {
            READY: 'READY',
            CHECKING_CONSISTENCY: 'CHECKING_CONSISTENCY'
        };

        $scope.consistencyChecking = {
            state: consistencyCheckingStates.READY,
            result: consistencyRepository.getLastConsistencyCheckingResult()
        };

        $scope.$watch('consistencyChecking.result', function (result) {
            consistencyRepository.setLastConsistencyCheckingResult(result);
        }, true);

        $scope.checkConsistency = function () {
            setState(consistencyCheckingStates.CHECKING_CONSISTENCY);
            setInconsistentGroups(null);
            $scope.processedGroupsPercent = 0;
            $scope.processedGroups = 0;

            consistencyRepository.listGroupNames()
                .then(function (groups) {

                    return partition(groups, config.maxGroupBatchSize)
                        .reduce(
                            (promise, groupsToCheck) => promise
                                .then(inconsistentGroups => listInconsistentGroups(groupsToCheck)
                                    .then(function (newInconsistentGroups) {
                                        $scope.processedGroups += groupsToCheck.length;
                                        $scope.processedGroupsPercent = Math.floor(($scope.processedGroups / groups.length) * 100);
                                        return inconsistentGroups.concat(newInconsistentGroups);
                                    })
                                ),
                            Promise.resolve([])
                        )
                        .then(setInconsistentGroups);
                })
                .catch(function (response) {
                    showErrorPopup('Cannot check consistency', response);
                    setInconsistentGroups(null);
                })
                .finally(function () {
                    setState(consistencyCheckingStates.READY);
                });
        };

        function showErrorPopup(message, response) {
            toaster.pop(
                'error',
                'Error ' + response.status,
                message + ' due to error: ' + response.status + ' ' + response.statusText
            );
        }

        function setState(inconsistentGroups) {
            $scope.consistencyChecking.state = inconsistentGroups;
        }

        function setInconsistentGroups(inconsistentGroups) {
            $scope.consistencyChecking.result = {
                inconsistentGroups: inconsistentGroups
            };
        }

        function partition(items, size) {
            const result = _.groupBy(items, function (item, i) {
                return Math.floor(i / size);
            });
            return _.values(result);
        }

        function listInconsistentGroups(groupsToCheck) {
            return consistencyRepository.listInconsistentGroups(groupsToCheck);
        }
    }]);

consistency.controller('GroupConsistencyController', ['$scope', '$stateParams', '$state', 'ConsistencyRepository',
    function ($scope, $stateParams, $state, consistencyRepository) {

        $scope.groupName = $stateParams.groupName;
        $scope.group = consistencyRepository.getGroup($scope.groupName);

        if (!$scope.group) {
            $state.go('consistency');
        }
    }]);

consistency.controller('TopicConsistencyController', ['$scope', '$stateParams', '$state', 'ConsistencyRepository',
    function ($scope, $stateParams, $state, consistencyRepository) {

        $scope.groupName = $stateParams.groupName;
        $scope.topicName = $stateParams.topicName;
        $scope.topic = consistencyRepository.getTopic($scope.groupName, $scope.topicName);

        if (!$scope.topic) {
            $state.go('consistency');
        }
    }]);
