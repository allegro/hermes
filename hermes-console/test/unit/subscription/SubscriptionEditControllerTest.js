describe("SubscriptionEditController", function() {

    var $controller, $httpBackend;

    beforeEach(angular.mock.module('hermes.subscription'));
    beforeEach(angular.mock.module('hermes.discovery'));
    beforeEach(angular.mock.module('toaster'));
    beforeEach(angular.mock.module('ngResource'));
    beforeEach(angular.mock.module(function($provide) {
        $provide.constant('HERMES_URLS', ["hermes_url"]);
        $provide.value("SUBSCRIPTION_CONFIG", {});
    }));

    beforeEach(inject(function(_$controller_, _$httpBackend_) {
        $controller = _$controller_;
        $httpBackend = _$httpBackend_;
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it("should edit subscription", function() {
        // given
        var subscription = {
            name: 'subscription',
            endpoint: "http://my-endpoint",
            description: "Initial description"
        };

        var $scope = {};
        $controller('SubscriptionEditController', {
            $scope: $scope,
            subscription: subscription,
            operation: "SAVE",
            groupName: 'group',
            topicName: 'topic',
            endpointAddressResolverMetadataConfig: {},
            topicContentType: {},
            showHeadersFilter: {},
            showFixedHeaders:{},
            $uibModalInstance: { close: function() {} }}
        );

        // when
        $scope.subscription.endpoint = "http://changed-endpoint";
        $scope.subscription.description = "Changed description";
        $httpBackend.expect('PUT', 'hermes_url/topics/topic/subscriptions/subscription', {
            name: 'subscription', endpoint: "http://changed-endpoint", description: "Changed description"
        }).respond(200, true);
        $scope.save();
        $httpBackend.flush();

        // then part is in afterEach()
    });

   it("should not send endpoint when it has not changed on save", function() {
        // given
        var subscription = {
            name: 'subscription',
            endpoint: "http://my-endpoint",
            description: "Initial description"
        };

        var $scope = {};
        $controller('SubscriptionEditController', {
            $scope: $scope,
            subscription: subscription,
            operation: "SAVE",
            groupName: 'group',
            topicName: 'topic',
            endpointAddressResolverMetadataConfig: {},
            topicContentType: {},
            showHeadersFilter: {},
            showFixedHeaders:{},
            $uibModalInstance: { close: function() {} }}
        );

        // when
        $scope.subscription.description = "Changed description";
        $httpBackend.expect('PUT',
            'hermes_url/topics/topic/subscriptions/subscription',
            {name: 'subscription', description: "Changed description"})
                .respond(200, true);
        $scope.save();
        $httpBackend.flush();

        // then part is in afterEach()
    });

});
