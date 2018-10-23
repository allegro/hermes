var subscriptions = angular.module('hermes.subscription', [
    'ui.bootstrap',
    'hermes.modals',
    'hermes.subscription.repository',
    'hermes.subscription.health',
    'hermes.subscription.metrics',
    'hermes.subscription.factory',
    'hermes.topic.metrics',
    'hermes.owner'
]);

subscriptions.controller('SubscriptionController', ['SubscriptionRepository', 'SubscriptionHealth', 'SubscriptionMetrics',
    'TopicRepository', 'TopicMetrics', '$scope', '$location', '$stateParams', '$uibModal', '$q', 'ConfirmationModal',
    'toaster', 'PasswordService', 'MessagePreviewModal', 'SUBSCRIPTION_CONFIG',
    function (subscriptionRepository, subscriptionHealth, subscriptionMetrics, topicRepository, topicMetrics,
              $scope, $location, $stateParams, $modal, $q, confirmationModal, toaster, passwordService,
              messagePreviewModal, config) {
        var groupName = $scope.groupName = $stateParams.groupName;
        var topicName = $scope.topicName = $stateParams.topicName;
        var subscriptionName = $scope.subscriptionName = $stateParams.subscriptionName;

        $scope.subscription = subscriptionRepository.get(topicName, subscriptionName);
        $scope.retransmissionLoading = false;

        $scope.endpointAddressResolverMetadataConfig = config.endpointAddressResolverMetadata;

        $scope.metricsUrls = subscriptionMetrics.metricsUrls(groupName, topicName, subscriptionName);

        topicRepository.get(topicName).then(function(topic) {
            $scope.topicContentType = topic.contentType;
            initRetransmissionCalendar(topic.retentionTime.duration);
        });

        subscriptionMetrics.metrics(topicName, subscriptionName).then(function(metrics) {
            $scope.metrics = metrics;
        });

        subscriptionHealth.health(topicName, subscriptionName).then(function(health) {
            $scope.health = health;
        });

        subscriptionRepository.lastUndelivered(topicName, subscriptionName).$promise
                .then(function (lastUndelivered) {
                    $scope.lastUndelivered = lastUndelivered;
                })
                .catch(function () {
                    $scope.lastUndelivered = null;
                });

        subscriptionRepository.undelivered(topicName, subscriptionName).$promise
            .then(function (undelivered) {
                $scope.undelivered = undelivered;
            })
            .catch(function () {
                $scope.undelivered = [];
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

        $scope.edit = function () {
            $modal.open({
                templateUrl: 'partials/modal/editSubscription.html',
                controller: 'SubscriptionEditController',
                size: 'lg',
                resolve: {
                    subscription: function () {
                        return $scope.subscription;
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
                    }
                }
            });
        };

        $scope.remove = function () {
            confirmationModal.open({
                action: 'Remove',
                actionSubject: 'Subscription ' + $scope.subscription.name,
                passwordLabel: 'Root password',
                passwordHint: 'root password'
            }).result.then(function (result) {
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
            var retransmitFromDate = $('#retransmitFromDate').val();

            if (retransmitFromDate === '') {
                toaster.pop('info', 'Info', 'Select date & time from which retransmission should be started');
                return;
            }

            confirmationModal.open({
                action: 'Retransmit',
                actionSubject: 'Subscription ' + $scope.subscription.name,
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

        $scope.trackingModeName = {"trackingOff": "No tracking", "discardedOnly": "Track message discarding only", "trackingAll": "Track everything"};

    }]);

subscriptions.controller('SubscriptionEditController', ['SubscriptionRepository', '$scope', '$uibModalInstance', 'subscription',
    'topicName', 'PasswordService', 'toaster', 'operation', 'endpointAddressResolverMetadataConfig', 'topicContentType',
    function (subscriptionRepository, $scope, $modal, subscription, topicName, passwordService, toaster, operation,
              endpointAddressResolverMetadataConfig, topicContentType) {
        $scope.topicName = topicName;
        $scope.topicContentType = topicContentType;
        $scope.subscription = subscription;
        $scope.operation = operation;
        $scope.endpointAddressResolverMetadataConfig = endpointAddressResolverMetadataConfig;

        var subscriptionBeforeChanges = _.cloneDeep(subscription);

        $scope.save = function () {
            var promise;
            passwordService.setRoot($scope.rootPassword);

            if (operation === 'ADD') {
                promise = subscriptionRepository.add(topicName, $scope.subscription).$promise;
            }
            else {
                var subscriptionToSave = _.cloneDeep(subscription);
                if(subscription.endpoint === subscriptionBeforeChanges.endpoint) {
                    delete subscriptionToSave.endpoint;
                }
                delete subscriptionToSave['oAuthPolicy']; // prevent from resetting password
                promise = subscriptionRepository.save(topicName, subscriptionToSave).$promise;
            }

            promise
                    .then(function () {
                        toaster.pop('success', 'Success', 'Subscription has been saved');
                        $modal.close();
                    })
                    .catch(function (response) {
                        toaster.pop('error', 'Error ' + response.status, response.data.message);
                    })
                    .finally(function () {
                        passwordService.reset();
                    });
        };

        $scope.addFilter = function () {
            if (!$scope.filter.path || !$scope.filter.matcher) {
                return;
            }

            $scope.subscription.filters.push({
                type: $scope.topicContentType === "JSON" ? "jsonpath" : "avropath",
                path: $scope.filter.path,
                matcher: $scope.filter.matcher
            });
            $scope.filter = {};
        };

        $scope.delFilter = function (index) {
            $scope.subscription.filters.splice(index, 1);
        };

    }]);

function initRetransmissionCalendar(daysBack) {
    var startDate = new Date();
    startDate.setDate(startDate.getDate() - daysBack);

    $('#retransmitCalendarButton').datetimepicker({
        format: "dd-MM-yyyy hh:ii",
        linkField: "retransmitFromDate",
        linkFormat: "yyyy-mm-ddThh:ii",
        showMeridian: true,
        autoclose: true,
        startDate: startDate,
        endDate: new Date(),
    });
}
