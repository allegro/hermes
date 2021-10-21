describe("GroupsController", function() {

    var $controller, $httpBackend;

    beforeEach(angular.mock.module('hermes.groups'));
    beforeEach(angular.mock.module('ngResource'));
    beforeEach(angular.mock.module(function($provide) {
        $provide.constant('HERMES_URLS', ["hermes_url"]);
        $provide.value("TOPIC_CONFIG", {});
        $provide.value("GROUP_CONFIG", {});
    }));

    beforeEach(inject(function(_$controller_, _$httpBackend_) {
        $controller = _$controller_;
        $httpBackend = _$httpBackend_;
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it("should fetch empty list of groups", function() {
        // given
        $httpBackend.when('GET', 'hermes_url/groups').respond([]);
        $httpBackend.when('GET', 'hermes_url/topics').respond([]);

        // when
        var $scope = {};
        $controller('GroupsController', {$scope: $scope});

        $httpBackend.flush();

        // then
        expect($scope.groups.length).toEqual(0);
        expect($scope.fetching).toEqual(false);
    });

    it("should fetch groups with corresponding topics", function() {

        // given
        $httpBackend.when('GET', 'hermes_url/groups').respond(['g1', 'g2', 'g3', 'g12']);
        $httpBackend.when('GET', 'hermes_url/topics').respond(['g1.t1', 'g1.t2', 'g3.t1', 'g12.t1']);


        // when
        var $scope = {};
        $controller('GroupsController', {$scope: $scope});
        $httpBackend.flush();

        // then
        expect($scope.groups).toEqual([{name: 'g1', topics: ['g1.t1', 'g1.t2']},
            {name: 'g2', topics: []},
            {name: 'g3', topics: ['g3.t1']},
            {name: 'g12', topics: ['g12.t1']}]);
        expect($scope.fetching).toEqual(false);

    });
});
