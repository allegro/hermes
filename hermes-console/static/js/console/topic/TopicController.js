var topics = angular.module('hermes.topic', [
    'ui.bootstrap',
    'toaster',
    'hermes.subscription',
    'hermes.topic.repository',
    'hermes.topic.metrics',
    'hermes.topic.factory',
    'hermes.services',
    'hermes.filters',
    'hermes.owner'
]);

topics.controller('TopicController', ['TOPIC_CONFIG', 'TopicRepository', 'TopicMetrics', '$scope', '$location', '$stateParams', '$uibModal',
    'ConfirmationModal', 'toaster', 'PasswordService', 'SubscriptionFactory', 'SUBSCRIPTION_CONFIG', 'OfflineClientsRepository',
    function (topicConfig, topicRepository, topicMetrics, $scope, $location, $stateParams, $modal, confirmationModal, toaster, passwordService,
              subscriptionFactory, subscriptionConfig, offlineClientsRepository) {
        var groupName = $scope.groupName = $stateParams.groupName;
        var topicName = $scope.topicName = $stateParams.topicName;

        $scope.subscriptionsFetching = true;
        $scope.offlineClientsFetching = true;
        $scope.showMessageSchema = false;
        $scope.config = topicConfig;

        topicRepository.get(topicName).then(function(topicWithSchema) {
            $scope.topic = topicWithSchema;
            $scope.topic.shortName = $scope.topic.name.substring($scope.topic.name.lastIndexOf('.') + 1);
            if (topicWithSchema && topicWithSchema.createdAt && topicWithSchema.modifiedAt) {
                var createdAt = new Date(0);
                createdAt.setUTCSeconds(topicWithSchema.createdAt);
                $scope.topic.createdAt = createdAt;

                var modifiedAt = new Date(0);
                modifiedAt.setUTCSeconds(topicWithSchema.modifiedAt);
                $scope.topic.modifiedAt = modifiedAt;
            }
            try {
                $scope.messageSchema = topicWithSchema.schema ? JSON.stringify(JSON.parse(topicWithSchema.schema), null, 2) : null;
            } catch (e) {
                console.error('Could not parse topic schema: ', e);
                $scope.messageSchema = '[schema parsing failure]';
            }
        });

        $scope.metricsUrls = topicMetrics.metricsUrls(groupName, topicName);
        topicMetrics.metrics(topicName).then(function(metrics) { $scope.metrics = metrics; });

        function loadSubscriptions() {
            topicRepository.listSubscriptionsWithDetails(topicName).then(function (subscriptions) {
                $scope.subscriptions = subscriptions;
                $scope.subscriptionsFetching = false;
            });
        }

        function loadBlacklistStatus() {
            topicRepository.blacklistStatus(topicName).then(function (blacklistStatus) {
                $scope.isBlacklisted = blacklistStatus.blacklisted;
            });
        }

        function loadOfflineClients() {
            offlineClientsRepository.get(topicName).then(function (clients) {
                $scope.clients = clients;
                $scope.offlineClientsFetching = false;
            });
        }

        loadSubscriptions();
        loadBlacklistStatus();
        if ($scope.config.offlineClientsEnabled) {
            loadOfflineClients();
        }

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

        $scope.clone = function() {
            $modal.open({
                templateUrl: 'partials/modal/editTopic.html',
                controller: 'TopicEditController',
                size: 'lg',
                resolve: {
                    operation: function () {
                        return 'ADD';
                    },
                    groupName: function () {
                        return groupName;
                    },
                    topic: function () {
                        return $scope.topic;
                    },
                    messageSchema: function() {
                        return $scope.messageSchema;
                    }
                }
            }).result.then(function(response){
                var topicName = response.topic.name;
                $location.path('/groups/' + groupName + '/topics/' + topicName);
            });
        };

        $scope.remove = function () {
            confirmationModal.open({
                action: 'Remove',
                actionSubject: 'Topic ' + $scope.topic.name,
                passwordLabel: 'Root password',
                passwordHint: 'root password'
            }).result.then(function (result) {
                    passwordService.setRoot(result.password);
                    var topic = $scope.topic;
                    topicRepository.remove($scope.topic)
                        .then(function () {
                            toaster.pop('success', 'Success', 'Topic has been removed');
                            if (!topicConfig.removeSchema && topic.contentType == 'AVRO') {
                                toaster.pop('warning', 'Topic schema was not removed',
                                    'Note that schema was not removed for this topic. Schema is persisted in an external registry ' +
                                    'and its removal is disabled in this environment. Before creating topic with the same name make sure ' +
                                    'it\'s manually removed by the schema registry operator.')
                            }
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
                passwordLabel: 'Root password',
                passwordHint: 'root password'
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
                passwordLabel: 'Root password',
                passwordHint: 'root password'
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
                    },
                    topicContentType: function () {
                        return $scope.topic.contentType;
                    }
                }
            }).result.then(function () {
                loadSubscriptions();
            });
        };
    }]);

topics.controller('TopicEditController', ['TOPIC_CONFIG', 'TopicRepository', '$scope', '$uibModalInstance', 'PasswordService',
    'toaster', 'topic', 'messageSchema', 'groupName', 'operation',
    function (topicConfig, topicRepository, $scope, $modal, passwordService, toaster, topic, messageSchema, groupName, operation) {
        $scope.config = topicConfig;

        $scope.topic = _(topic).clone();
        $scope.messageSchema = messageSchema;
        $scope.groupName = groupName;
        $scope.operation = operation;

        $scope.save = function () {
            var promise;
            var originalTopicName = $scope.topic.name;
            passwordService.setRoot($scope.rootPassword);

            var topic = _.cloneDeep($scope.topic);
            delete topic.shortName;

            if (operation === 'ADD') {
                topic.name = groupName + '.' + $scope.topic.shortName;
                $scope.topic.name = topic.name;
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
