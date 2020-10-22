var repository = angular.module('hermes.consistency.repository', []);

repository.factory('ConsistencyRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {

        const consistencyResource = $resource(discovery.resolve('/consistency/inconsistencies/groups'));
        const groupsResource = $resource(discovery.resolve('/consistency/groups'));

        var lastConsistencyCheckingResult = {
            inconsistentGroups: null
        };

        function getGroup(groupName) {
            return _.find(lastConsistencyCheckingResult.inconsistentGroups, {'name': groupName});
        }

        return {
            listInconsistentGroups: function (groupNames) {
                return consistencyResource.query({groupNames: groupNames}).$promise;
            },
            listGroupNames: function () {
                return groupsResource.query().$promise;
            },
            setLastConsistencyCheckingResult: function (result) {
                lastConsistencyCheckingResult = result;
            },
            getLastConsistencyCheckingResult: function () {
                return lastConsistencyCheckingResult;
            },
            getGroup: getGroup,
            getTopic: function (groupName, topicName) {
                const group = getGroup(groupName) || {};
                return _.find(group.inconsistentTopics, {'name': topicName});
            }
        };
    }]);
