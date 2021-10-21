
describe("GroupEditController", function() {
    var $controller, $httpBackend;

    beforeEach(angular.mock.module('hermes.groups'));
    beforeEach(angular.mock.module('ngResource'));
    beforeEach(angular.mock.module(function($provide) {
        $provide.constant('HERMES_URLS', ["hermes_url"]);
        $provide.value("TOPIC_CONFIG", {});

    }));

    beforeEach(inject(function(_$controller_, _$httpBackend_) {
        $controller = _$controller_;
        $httpBackend = _$httpBackend_;
    }));
    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it("should create group", function() {
        // given
        var group = {
            "groupName": "someGroup"
        };

        $httpBackend.when('POST', 'hermes_url/groups', group).respond(201, true);

        // when
        var $scope = {};
        $controller('GroupEditController', {$scope: $scope, group: group, operation: "ADD", $uibModalInstance: { close: function() {} }});
        $scope.save();

        $httpBackend.flush();

        // then part is in afterEach()
    });

});
