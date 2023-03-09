var groups = angular.module('hermes.groups', ['hermes.topic', 'hermes.discovery', 'ui.bootstrap']);

groups.controller('GroupsController', ['GROUP_CONFIG', 'GroupRepository', '$scope', '$rootScope', '$uibModal',
    function (groupConfig, groupRepository, $scope, $rootScope, $modal) {
        $scope.fetching = true;
        $scope.search = groupRepository.getSearchFilter();

        $scope.canCreateGroup = function() {
            return $rootScope.admin || groupConfig.nonAdminCreationEnabled;
        };

        function loadGroups() {
            groupRepository.list().then(function (groups) {
                $scope.groups = groups;
                $scope.fetching = false;
            });
        }

        loadGroups();

        $scope.addGroup = function () {
            $modal.open({
                templateUrl: 'partials/modal/editGroup.html',
                controller: 'GroupEditController',
                size: 'lg',
                backdrop: 'static',
                resolve: {
                    group: function() {
                        return {};
                    },
                    operation: function() {
                        return 'ADD';
                    }
                }
            }).result.then(function () {
                loadGroups();
            });
        };

        $scope.storeSearchFilter = function () {
            groupRepository.storeSearchFilter($scope.search);
        };
    }]);

groups.controller('GroupController', ['GroupRepository', 'TopicFactory', '$scope', '$location', '$stateParams', '$uibModal', 'toaster', 'ConfirmationModal', 'PasswordService',
    function (groupRepository, topicFactory, $scope, $location, $stateParams, $modal, toaster, confirmationModal, passwordService) {
        $scope.fetching = true;
        var groupName = $scope.groupName = $stateParams.groupName;

        $scope.group = groupRepository.get(groupName);
        $scope.topics = [];

        $scope.showTopic = function(topicName) {
            $location.path('/groups/' + groupName + '/topics/' + topicName);
        };

        $scope.fetchSubscriptions = function(topic) {
            groupRepository.listSubscriptions(topic.name).then(function(subscriptions) {
                topic.subscriptions = subscriptions;
            });
        };

        $scope.addTopic = function() {
            $modal.open({
                templateUrl: 'partials/modal/editTopic.html',
                controller: 'TopicEditController',
                size: 'lg',
                backdrop: 'static',
                resolve: {
                    operation: function () {
                        return 'ADD';
                    },
                    groupName: function () {
                        return groupName;
                    },
                    topic: function () {
                        return topicFactory.create();
                    },
                    messageSchema: function() {
                        return null;
                    }
                }
            }).result.then(function(){
                loadTopics();
            });


        };

        $scope.edit = function() {
            $modal.open({
                templateUrl: 'partials/modal/editGroup.html',
                controller: 'GroupEditController',
                size: 'lg',
                backdrop: 'static',
                resolve: {
                    group: function() {
                        return $scope.group;
                    },
                    operation: function () {
                        return 'EDIT';
                    }
                }
            });
        };

        $scope.remove = function() {
            confirmationModal.open({
                action: 'Remove',
                actionSubject: 'Group ' + $scope.group.groupName,
                passwordLabel: 'Root password',
                passwordHint: 'root password '
            }).result.then(function (result) {
                    $scope.disableRemoveButton = true;
                    passwordService.setRoot(result.password);
                    groupRepository.remove($scope.group.groupName).$promise
                        .then(function () {
                            toaster.pop('success', 'Success', 'Group has been removed');
                            $location.path('/groups').replace();
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                        })
                        .finally(function () {
                            passwordService.reset();
                            $scope.disableRemoveButton = false;
                        });
                });
        };

        loadTopics = function() {
                groupRepository.listTopics(groupName).then(function (topics) {
                $scope.topics = topics;
                $scope.fetching = false;
            });
        };
        loadTopics();
    }]);

groups.controller('GroupEditController', ['GroupRepository', '$scope', '$uibModalInstance', 'group', 'PasswordService', 'toaster', 'operation',
    function (groupRepository, $scope, $modal, group, passwordService, toaster, operation) {
        $scope.group = group;
        $scope.operation = operation;

        $scope.save = function () {
            $scope.disableSaveButton = true;
            passwordService.setRoot($scope.rootPassword);
            var response = operation === 'ADD'? groupRepository.add($scope.group) : groupRepository.save($scope.group);
            response.$promise
                .then(function () {
                    toaster.pop('success', 'Success', 'Group has been saved');
                    $modal.close();
                })
                .catch(function (response) {
                    toaster.pop('error', 'Error ' + response.status, response.data.message);
                })
                .finally(function () {
                    passwordService.reset();
                    $scope.disableSaveButton = false;
                });
        };
    }]);

groups.factory('GroupRepository', ['$resource', '$q', '$location', 'TopicRepository', 'DiscoveryService',
    function ($resource, $q, $location, topicRepository, discovery) {
        var repository = $resource(discovery.resolve('/groups/:name'), null, {update: {method: 'PUT'}});
        var listing = $resource(discovery.resolve('/groups'));

        var filterGroupTopics = function (groupName, topics) {
            return _.filter(topics, function (topic) {
                return topic.indexOf(groupName) === 0 && groupName.length === topic.lastIndexOf('.');
            });
        };

        var searchFilter = '';

        return {
            list: function () {
                return $q.all([listing.query().$promise, topicRepository.list().$promise]).then(function (data) {
                    return _.map(data[0], function (group) {
                        return {name: group, topics: filterGroupTopics(group, data[1])};
                    });
                });
            },
            get: function (name) {
                return repository.get({name: name}, function() {}, function(e) {
                    if(e.status == 404) { $location.path('/groups'); }
                });
            },
            add: function (group) {
                return listing.save({}, group);
            },
            save: function(group) {
                return repository.update({name: group.groupName}, group);
            },
            remove: function(groupName) {
                return repository.remove({name: groupName});
            },
            listTopics: function (name) {
                return topicRepository.list().$promise.then(_.curry(filterGroupTopics)(name)).then(function (topics) {
                    return _.map(topics, function (topic) {
                        return { name: topic };
                    });
                });
            },
            listSubscriptions: function(topicName) {
                return topicRepository.listSubscriptions(topicName).$promise;
            },
            storeSearchFilter: function (filter) {
                searchFilter = filter;
            },
            getSearchFilter: function () {
                return searchFilter;
            }
        };
    }]);
