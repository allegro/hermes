var topics = angular.module('hermes.topic', [
    'ui.bootstrap',
    'toaster',
    'hermes.subscription',
    'hermes.topic.repository',
    'hermes.topic.metrics',
    'hermes.topic.factory',
    'hermes.services'
]);

topics.controller('TopicController', ['TOPIC_CONFIG', 'TopicRepository', 'TopicMetrics', '$scope', '$location', '$stateParams', '$uibModal',
    'ConfirmationModal', 'toaster', 'PasswordService', 'SubscriptionFactory', 'SUBSCRIPTION_CONFIG',
    function (topicConfig, topicRepository, topicMetrics, $scope, $location, $stateParams, $modal, confirmationModal, toaster, passwordService,
              subscriptionFactory, subscriptionConfig) {
        var groupName = $scope.groupName = $stateParams.groupName;
        var topicName = $scope.topicName = $stateParams.topicName;

        $scope.fetching = true;
        $scope.showMessageSchema = false;
        $scope.previewEnabled = topicConfig.messagePreviewEnabled;

        topicRepository.get(topicName).then(function(topicWithSchema) {
            $scope.topic = topicWithSchema.topic;
            $scope.messageSchema = topicWithSchema.messageSchema;
        });

        $scope.metricsUrls = topicMetrics.metricsUrls(groupName, topicName);
        topicMetrics.metrics(topicName).then(function(metrics) { $scope.metrics = metrics; });

        function loadSubscriptions() {
            topicRepository.listSubscriptionsWithDetails(topicName).then(function (subscriptions) {
                $scope.subscriptions = subscriptions;
                $scope.fetching = false;
            });
        }

        function loadBlacklistStatus() {
            topicRepository.blacklistStatus(topicName).then(function (blacklistStatus) {
                $scope.isBlacklisted = blacklistStatus.blacklisted;
            });
        }

        loadSubscriptions();
        loadBlacklistStatus();

        topicRepository.preview(topicName).then(function(preview) {
            $scope.preview = preview;
        });

        $scope.edit = function () {
            $modal.open({
                templateUrl: 'partials/modal/editTopic.html',
                controller: 'TopicEditController',
                size: 'lg',
                resolve: {
                    operation: function () {
                        return 'EDIT';
                    },
                    topic: function () {
                        return $scope.topic;
                    },
                    messageSchema: function() {
                        return $scope.messageSchema;
                    },
                    groupName: function () {
                        return groupName;
                    }
                }
            }).result.then(function (result) {
                $scope.topic = result.topic;
                $scope.messageSchema = result.messageSchema;
            });
        };

        $scope.remove = function () {
            confirmationModal.open({
                action: 'Remove',
                actionSubject: 'Topic ' + $scope.topic.name,
                passwordLabel: 'Group password',
                passwordHint: 'Password for group ' + groupName
            }).result.then(function (result) {
                    passwordService.set(result.password);
                    topicRepository.remove($scope.topic)
                        .then(function () {
                            toaster.pop('success', 'Success', 'Topic has been removed');
                            $location.path('/groups/' + groupName).replace();
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                        })
                        .finally(function () {
                            passwordService.reset();
                        });
                });
        };

        $scope.blacklist = function () {
            confirmationModal.open({
                action: 'Blacklist',
                actionSubject: 'Topic ' + $scope.topic.name,
                passwordLabel: 'Group password',
                passwordHint: 'Password for group ' + groupName
            }).result.then(function () {
                topicRepository.blacklist(topicName).$promise
                    .then(function () {
                        toaster.pop('success', 'Success', 'Topic has been blacklisted');
                    })
                    .catch(function (response) {
                        toaster.pop('error', 'Error ' + response.status, response.data.message);
                    })
                    .finally(function () {
                        loadBlacklistStatus();
                    });
            });
        };

        $scope.unblacklist = function () {
            confirmationModal.open({
                action: 'Unblacklist',
                actionSubject: 'Topic ' + $scope.topic.name,
                passwordLabel: 'Group password',
                passwordHint: 'Password for group ' + groupName
            }).result.then(function () {
                topicRepository.unblacklist(topicName).$promise
                    .then(function () {
                        toaster.pop('success', 'Success', 'Topic has been unblacklisted');
                    })
                    .catch(function (response) {
                        toaster.pop('error', 'Error ' + response.status, response.data.message);
                    })
                    .finally(function () {
                        loadBlacklistStatus();
                    });
            });
        };

        $scope.addSubscription = function () {
            $modal.open({
                templateUrl: 'partials/modal/editSubscription.html',
                controller: 'SubscriptionEditController',
                size: 'lg',
                resolve: {
                    operation: function () {
                        return 'ADD';
                    },
                    topicName: function () {
                        return topicName;
                    },
                    subscription: function () {
                        return subscriptionFactory.create(topicName);
                    },
                    endpointAddressResolverMetadataConfig: function() {
                        return subscriptionConfig.endpointAddressResolverMetadata;
                    }
                }
            }).result.then(function () {
                loadSubscriptions();
            });
        };
    }]);

topics.controller('TopicEditController', ['TopicRepository', '$scope', '$uibModalInstance', 'PasswordService', 'toaster', 'topic', 'messageSchema', 'groupName', 'operation',
    function (topicRepository, $scope, $modal, passwordService, toaster, topic, messageSchema, groupName, operation) {
        $scope.topic = _(topic).clone();
        $scope.messageSchema = messageSchema;
        $scope.groupName = groupName;
        $scope.operation = operation;

        $scope.save = function () {
            var promise;
            var originalTopicName = $scope.topic.name;
            passwordService.set($scope.groupPassword);

            var topic = _.cloneDeep($scope.topic);
            delete topic.shortName;

            if (operation === 'ADD') {
                topic.name = groupName + '.' + $scope.topic.shortName;
                promise = topicRepository.add(topic, $scope.messageSchema);
            }
            else {
                promise = topicRepository.save(topic, $scope.messageSchema);
            }

            promise
                    .then(function () {
                        toaster.pop('success', 'Success', 'Topic has been saved');
                        $modal.close({topic: $scope.topic, messageSchema: $scope.messageSchema});
                    })
                    .catch(function (response) {
                        toaster.pop('error', 'Error ' + response.status, response.data.message);
                        $scope.topic.name = originalTopicName;
                    })
                    .finally(function () {
                        passwordService.reset();
                    });
        };
    }]);
