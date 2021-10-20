describe("GroupController", function() {

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

    beforeEach(function () {
        // given

        $scope = {};
        $stateParams = {
            "groupName": "someGroup"
        };
        controller = $controller('GroupController', {$scope: $scope, $stateParams: $stateParams});

    });

    it("should fetch group with all properties", function() {
        // when

        $httpBackend.when('GET', 'hermes_url/groups/someGroup').respond("group");
        $httpBackend.when('GET', 'hermes_url/groups').respond(['someGroup']);
        $httpBackend.when('GET', 'hermes_url/topics').respond([]);

        // then
        $httpBackend.flush();
        expect($scope.fetching).toEqual(false);
        expect($scope.groupName).toEqual($stateParams.groupName);
    });
});
