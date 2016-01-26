var messagePreview = angular.module('hermes.messagePreview', ['ui.bootstrap', 'hermes.services', 'toaster', 'hermes.modals']);

messagePreview.controller('MessagePreviewController', ['$scope', 'topicName', 'partition', 'offset', 'previewedMessage',
    function ($scope, topicName, partition, offset, previewedMessage) {
        $scope.previewedMessage = previewedMessage;
        $scope.topicName = topicName;
        $scope.partition = partition;
        $scope.offset = offset;


    }
]);

messagePreview.factory('MessagePreviewModal', ['$uibModal', 'toaster', 'ConfirmationModal', 'PasswordService', 'MessagePreviewRepository',
    function ($modal, toaster, confirmationModal, passwordService, messagePreviewRepository) {
    return {
        previewMessage: function (topicName, cluster, partition, offset, messageId) {
            confirmationModal.open({
                action: 'Preview',
                actionSubject: messageId,
                passwordLabel: 'Superuser password'
            }).result.then(function (result) {
                    passwordService.setRoot(result.password);
                    messagePreviewRepository.messagePreview(topicName, cluster, partition, offset).$promise
                        .then(function (previewedMessage) {
                            openModal(topicName, partition, offset, previewedMessage);
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, 'Cannot get message preview due to error: ' + response.status + ' ' + response.statusText);
                        })
                        .finally(function () {
                            passwordService.reset();
                        });
                });

            function openModal(topicName, partition, offset, previewedMessage) {
                $modal.open({
                    templateUrl: 'partials/modal/messagePreview.html',
                    controller: 'MessagePreviewController',
                    size: 'lg',
                    resolve: {
                        previewedMessage: function () {
                            return previewedMessage;
                        },
                        topicName: function () {
                            return topicName;
                        },
                        partition: function () {
                            return partition;
                        },
                        offset: function () {
                            return offset;
                        }
                    }
                });
            }
        }
    };
}]);


messagePreview.factory('MessagePreviewRepository', ['DiscoveryService', '$resource',
    function (discovery, $resource) {
        var messagePreview = $resource(discovery.resolve('/topics/:name/preview/cluster/:cluster/partition/:partition/offset/:offset'));
        return {
            messagePreview: function(topicName, cluster, partition, offset) {
                return messagePreview.get({name: topicName, cluster: cluster, partition: partition, offset: offset});
            }
        };
    }]);
