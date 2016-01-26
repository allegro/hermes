describe("SubscriptionEditController", function() {

    function hermesUrl(path) {
        return 'http://hermes.allegro.tech' + path;
    }

    beforeEach(module('hermes', function(_$provide_){
        $provide = _$provide_;
    }));

    beforeEach(inject(function(_$controller_, _$httpBackend_){
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
            $modalInstance: { close: function() {} }}
        );

        // when
        subscription.endpoint = "http://changed-endpoint";
        subscription.description = "Changed description";
        $httpBackend.expect('PUT', hermesUrl('/topics/topic/subscriptions/subscription'), {
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
            $modalInstance: { close: function() {} }}
        );

        // when
        subscription.description = "Changed description";
        $httpBackend.expect('PUT', hermesUrl('/topics/topic/subscriptions/subscription'), {name: 'subscription', description: "Changed description"}).respond(200, true);
        $scope.save();
        $httpBackend.flush();

        // then part is in afterEach()
    });

});
