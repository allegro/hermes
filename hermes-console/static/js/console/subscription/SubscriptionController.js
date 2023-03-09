var subscriptions = angular.module('hermes.subscription', [
    'ui.bootstrap',
    'hermes.modals',
    'hermes.subscription.repository',
    'hermes.subscription.health',
    'hermes.subscription.metrics',
    'hermes.subscription.factory',
    'hermes.topic.metrics',
    'hermes.filters.debugger',
    'hermes.owner'
]);

subscriptions.controller('SubscriptionController', ['SubscriptionRepository', 'SubscriptionHealth', 'SubscriptionMetrics',
    'TopicRepository', 'TopicMetrics', '$scope', '$location', '$stateParams', '$uibModal', '$q', 'ConfirmationModal',
    'toaster', 'PasswordService', 'MessagePreviewModal', 'FiltersDebuggerModalFactory', 'SUBSCRIPTION_CONFIG',
    function (subscriptionRepository, subscriptionHealth, subscriptionMetrics, topicRepository, topicMetrics,
              $scope, $location, $stateParams, $modal, $q, confirmationModal, toaster, passwordService,
              messagePreviewModal, filtersDebuggerModal, config) {
        var groupName = $scope.groupName = $stateParams.groupName;
        var topicName = $scope.topicName = $stateParams.topicName;
        var subscriptionName = $scope.subscriptionName = $stateParams.subscriptionName;
        $scope.config = config;

        function getUndelivered() {
            subscriptionRepository.undelivered(topicName, subscriptionName).$promise
                .then(function (undelivered) {
                    $scope.undelivered = undelivered;
                })
                .catch(function () {
                    $scope.undelivered = [];
                });
        }

        function getLastUndelivered() {
            subscriptionRepository.lastUndelivered(topicName, subscriptionName).$promise
                .then(function (lastUndelivered) {
                    $scope.lastUndelivered = lastUndelivered;
                })
                .catch(function () {
                    $scope.lastUndelivered = null;
                });
        }


        subscriptionRepository.get(topicName, subscriptionName).$promise
                .then(function(subscription) {
                    $scope.subscription = subscription;
                    if (subscription && subscription.createdAt && subscription.modifiedAt) {
                        var createdAt = new Date(0);
                        createdAt.setUTCSeconds(subscription.createdAt);
                        $scope.subscription.createdAt = createdAt;

                        var modifiedAt = new Date(0);
                        modifiedAt.setUTCSeconds(subscription.modifiedAt);
                        $scope.subscription.modifiedAt = modifiedAt;
                    }
                });

        $scope.retransmissionLoading = false;

        $scope.showHeadersFilter = config.showHeadersFilter;
        $scope.showFixedHeaders = config.showFixedHeaders;

        $scope.endpointAddressResolverMetadataConfig = config.endpointAddressResolverMetadata;

        $scope.metricsUrls = subscriptionMetrics.metricsUrls(groupName, topicName, subscriptionName);

        topicRepository.get(topicName).then(function(topic) {
            $scope.topicContentType = topic.contentType;
            $scope.daysBack = topic.retentionTime.duration;
        });

        subscriptionMetrics.metrics(topicName, subscriptionName).then(function(metrics) {
            $scope.metrics = metrics;
        });

        subscriptionHealth.health(topicName, subscriptionName).then(function(health) {
            $scope.health = health;
        });

        $scope.$watch('userHasSufficientPrivileges', function (currentValue) {
            // user has privileges to see undelivered messages
            if (currentValue === true) {
                getUndelivered();
                getLastUndelivered();
            }
        });

        $scope.notSupportedEndpointAddressResolverMetadataEntries = function(metadataEntries) {
          var filtered = {};
          _.each(metadataEntries, function(entry, key) {
              if (key in config.endpointAddressResolverMetadata === false) {
                  filtered[key] = entry;
              }
          });
          return filtered;
        };

        $scope.edit = function (subscription) {
            $modal.open({
                templateUrl: 'partials/modal/editSubscription.html',
                controller: 'SubscriptionEditController',
                size: 'lg',
                backdrop: 'static',
                resolve: {
                    subscription: function () {
                        return _.cloneDeep(subscription);
                    },
                    topicName: function () {
                        return topicName;
                    },
                    operation: function () {
                        return 'EDIT';
                    },
                    endpointAddressResolverMetadataConfig: function() {
                        return config.endpointAddressResolverMetadata;
                    },
                    topicContentType: function () {
                        return $scope.topicContentType;
                    },
                    showFixedHeaders: function () {
                        return $scope.showFixedHeaders;
                    },
                    showHeadersFilter: function () {
                      return $scope.showHeadersFilter;
                    }
                }
            }).result.then(function(response){
                $scope.subscription = response.subscription;
            });
        };

        $scope.clone = function () {
            $modal.open({
                templateUrl: 'partials/modal/editSubscription.html',
                controller: 'SubscriptionEditController',
                size: 'lg',
                backdrop: 'static',
                resolve: {
                    subscription: function () {
                        return $scope.subscription;
                    },
                    topicName: function () {
                        return topicName;
                    },
                    operation: function () {
                        return 'ADD';
                    },
                    endpointAddressResolverMetadataConfig: function() {
                        return config.endpointAddressResolverMetadata;
                    },
                    topicContentType: function () {
                        return $scope.topicContentType;
                    },
                    showFixedHeaders: function () {
                        return $scope.showFixedHeaders;
                    },
                    showHeadersFilter: function () {
                        return $scope.showHeadersFilter;
                    }
                }
            }).result.then(function(response){
                var subscriptionName = response.subscription.name;
                $location.path('/groups/' + groupName + '/topics/' + topicName + '/subscriptions/' + subscriptionName);
            });
        };

        $scope.remove = function () {
            confirmationModal.open({
                action: 'Remove',
                actionSubject: 'Subscription ' + $scope.subscription.name,
                passwordLabel: 'Root password',
                passwordHint: 'root password'
            }).result.then(function (result) {
                $scope.disableRemoveButton = true;
                passwordService.setRoot(result.password);
                subscriptionRepository.remove(topicName, $scope.subscription.name).$promise
                        .then(function () {
                            toaster.pop('success', 'Success', 'Subscription has been removed');
                            $location.path('/groups/' + groupName + '/topics/' + topicName).replace();
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

        $scope.suspend = function () {
            confirmationModal.open({
                action: 'Suspend',
                actionSubject: 'Subscription '+ $scope.subscription.name,
                passwordLabel: 'Root password',
                passwordHint: 'root password'
            }).result.then(function (result) {
                passwordService.setRoot(result.password);
                subscriptionRepository.suspend(topicName, $scope.subscription.name).$promise
                        .then(function () {
                            $scope.subscription.state = 'SUSPENDED';
                            toaster.pop('success', 'Success', 'Subscription has been suspended');
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                        })
                        .finally(function () {
                            passwordService.reset();
                        });
            });
        };

        $scope.activate = function () {
            confirmationModal.open({
                action: 'Activate',
                actionSubject: 'Subcription ' + $scope.subscription.name,
                passwordLabel: 'Root password',
                passwordHint: 'root password'
            }).result.then(function (result) {
                passwordService.setRoot(result.password);
                subscriptionRepository.activate(topicName, $scope.subscription.name).$promise
                        .then(function () {
                            $scope.subscription.state = 'ACTIVE';
                            toaster.pop('success', 'Success', 'Subscription has been activated');
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                        })
                        .finally(function () {
                            passwordService.reset();
                        });
            });
        };

        $scope.previewMessage = function(cluster, partition, offset, messageId) {
            messagePreviewModal.previewMessage(topicName, cluster, partition, offset, messageId);
        };

        $scope.showTrace = function() {
            subscriptionRepository.eventTrace(topicName, subscriptionName, $scope.eventId).$promise
                .then(function (trace) {
                    $scope.eventTrace = trace;
                })
                .catch(function () {
                    $scope.eventTrace = [];
                });
        };

        $scope.retransmit = function() {
            var retransmitFromDate = $('#retransmitFromDate').data().date;

            if (!retransmitFromDate) {
                toaster.pop('info', 'Info', 'Select date & time from which retransmission should be started');
                return;
            }

            confirmationModal.open({
                action: 'Retransmit',
                actionSubject: 'Subscription ' + $scope.subscription.name,
                additionalInfo: "This action re-send all messages starting from given date. Do you want to continue?",
                passwordLabel: 'Root password',
                passwordHint: 'root password'
            }).result.then(function (result) {
                    passwordService.setRoot(result.password);
                    $scope.retransmissionLoading = true;
                    subscriptionRepository.retransmit(topicName, $scope.subscription.name, retransmitFromDate).$promise
                        .then(function () {
                            toaster.pop('success', 'Success', 'Retransmission started');
                            $scope.retransmissionLoading = false;
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                            $scope.retransmissionLoading = false;
                        });
                });
        };
        $scope.skipMessages = function (){
            tommorowDate = new Date()
            tommorowDate.setDate(tommorowDate.getDate() + 1)

            confirmationModal.open({
                action: 'Skip messages',
                actionSubject: 'Subscription ' + $scope.subscription.name,
                additionalInfo: "This action will skip all undelivered messages for this subscription. Do you want to continue?",
                passwordLabel: 'Root password',
                passwordHint: 'root password'
            }).result.then(function (result) {
                passwordService.setRoot(result.password);
                $scope.skipMessagesLoading = true;
                subscriptionRepository.retransmit(topicName, $scope.subscription.name, tommorowDate.toISOString()).$promise
                    .then(function () {
                        toaster.pop('success', 'Success', 'Skipped messages.');
                        $scope.skipMessagesLoading = false;
                    })
                    .catch(function (response) {
                        toaster.pop('error', 'Error ' + response.status, response.data.message);
                        $scope.skipMessagesLoading = false;
                    });
            });

        }
        $scope.debugFilters = function () {
            filtersDebuggerModal.open(topicName, $scope.subscription.filters, $scope.topicContentType)
                .then(function (result) {
                    var subscription = _.cloneDeep($scope.subscription);
                    subscription.filters = result.messageFilters;
                    $scope.edit(subscription);
                });
        };

        $scope.trackingModeName = {"trackingOff": "No tracking", "discardedOnly": "Track message discarding only", "trackingAll": "Track everything"};

    }]);

subscriptions.controller('SubscriptionEditController', ['SubscriptionRepository', '$scope', '$uibModalInstance', 'subscription',
    'topicName', 'PasswordService', 'toaster', 'operation', 'endpointAddressResolverMetadataConfig', 'topicContentType',
    'showFixedHeaders', 'showHeadersFilter', 'SUBSCRIPTION_CONFIG',
    function (subscriptionRepository, $scope, $modal, subscription, topicName, passwordService, toaster, operation,
              endpointAddressResolverMetadataConfig, topicContentType, showFixedHeaders, showHeadersFilter, subscriptionConfig) {
        $scope.topicName = topicName;
        $scope.topicContentType = topicContentType;
        $scope.subscription = _.cloneDeep(subscription);
        $scope.operation = operation;
        $scope.endpointAddressResolverMetadataConfig = endpointAddressResolverMetadataConfig;
        $scope.showFixedHeaders = showFixedHeaders;
        $scope.showHeadersFilter = showHeadersFilter;
        $scope.config = subscriptionConfig;

        $scope.save = function () {
            $scope.disableSaveButton = true;
            var promise;
            var subscriptionToSave = _.cloneDeep($scope.subscription);
            passwordService.setRoot($scope.rootPassword);

            if (operation === 'ADD') {
                promise = subscriptionRepository.add(topicName, subscriptionToSave).$promise;
            } else {
                if(subscription.endpoint === subscriptionToSave.endpoint) {
                    delete subscriptionToSave.endpoint;
                }
                delete subscriptionToSave.oAuthPolicy; // prevent from resetting password
                promise = subscriptionRepository.save(topicName, subscriptionToSave).$promise;
            }

            promise
                    .then(function () {
                        toaster.pop('success', 'Success', 'Subscription has been saved');
                        $modal.close({ subscription: $scope.subscription });
                    })
                    .catch(function (response) {
                        toaster.pop('error', 'Error ' + response.status, response.data.message);
                    })
                    .finally(function () {
                        passwordService.reset();
                        $scope.disableSaveButton = false;
                    });
        };

        $scope.addHeader = function() {
            if(!$scope.header.name || !$scope.header.value) {
                return;
            }
            $scope.subscription.headers.push(angular.copy($scope.header));
            $scope.header = {};
        };

        $scope.delHeader = function (index) {
            $scope.subscription.headers.splice(index, 1);
        };

    }]);