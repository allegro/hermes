var consistency = angular.module('hermes.consistency', [
    'ui.bootstrap',
    'hermes.consistency.repository'
]);

consistency.controller('ConsistencyController', ['$scope', '$state', 'toaster', 'CONSISTENCY_CONFIG', 'ConsistencyRepository', 'ConfirmationModal',
    function ($scope, $state, toaster, config, consistencyRepository, confirmationModal) {

        const consistencyCheckingStates = {
            READY: 'READY',
            CHECKING_CONSISTENCY: 'CHECKING_CONSISTENCY'
        };

        $scope.consistencyChecking = {
            state: consistencyCheckingStates.READY,
            result: consistencyRepository.getLastConsistencyCheckingResult()
        };

        $scope.topicsConsistencyChecking = {
            state: consistencyCheckingStates.READY,
            result: consistencyRepository.getLastTopicsConsistencyCheckingResult()
        };

        $scope.$watch('consistencyChecking.result', function (result) {
            consistencyRepository.setLastConsistencyCheckingResult(result);
        }, true);

        $scope.$watch('topicsConsistencyChecking.result', function (result) {
            consistencyRepository.setLastTopicsConsistencyCheckingResult(result);
        }, true);

        $scope.checkConsistency = function() {
            checkGroupConsistency();
            findTopicsNotPresentInHermes();
        };

        $scope.removeTopic = function (topicName) {
            confirmationModal.open({
                actionSubject: 'Are you sure you want to remove topic: ' + topicName,
                action: "Remove"
            }).result.then(function () {
                consistencyRepository.removeTopic(topicName);
            })
            .then(function () {
                let newArray = $scope.topicsConsistencyChecking.result.filter(
                    function (element) {
                        return element !== topicName;
                    });
                setInconsistentTopics(newArray);
                toaster.pop('success', 'Success', 'Topic has been removed');
            })
            .catch(function (e) {
                showErrorPopup("cannot remove topic: " + e);
                setInconsistentTopics(null);
            });
        };

        function checkGroupConsistency() {
            setGroupsState(consistencyCheckingStates.CHECKING_CONSISTENCY);
            setInconsistentGroups(null);
            setInconsistentTopics(null);
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
                    setGroupsState(consistencyCheckingStates.READY);
                });
        }

        function findTopicsNotPresentInHermes() {
            setTopicsState(consistencyCheckingStates.CHECKING_CONSISTENCY);
            consistencyRepository.listInconsistentTopics()
            .then(function (topics) {
                setInconsistentTopics(topics);
            })
            .finally(function () {
                setTopicsState(consistencyCheckingStates.READY);
            });
        }

        function showErrorPopup(message, response) {
            toaster.pop(
                'error',
                'Error ' + response.status,
                message + ' due to error: ' + response.status + ' ' + response.statusText
            );
        }

        function setGroupsState(inconsistentGroupsState) {
            $scope.consistencyChecking.state = inconsistentGroupsState;
        }

        function setTopicsState(inconsistentGroupsState) {
            $scope.topicsConsistencyChecking.state = inconsistentGroupsState;
        }

        function setInconsistentGroups(inconsistentGroups) {
            $scope.consistencyChecking.result = inconsistentGroups
        }

        function setInconsistentTopics(inconsistentTopics) {
            $scope.topicsConsistencyChecking.result =inconsistentTopics
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
            $state.go('groupConsistency',{groupName: $scope.groupName});
        }
    }]);
