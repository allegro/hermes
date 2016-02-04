describe("GroupsController", function() {

    var $provide, $httpBackend, $controller;

    function hermesUrl(path) {
        return 'http://hermes.allegro.tech' + path;
    }

    beforeEach(module('hermes', function(_$provide_){
        $provide = _$provide_;
        $provide.value('HERMES_URL', hermesUrl(''));
    }));

    beforeEach(inject(function(_$controller_, _$httpBackend_){
        $controller = _$controller_;
        $httpBackend = _$httpBackend_;
    }));

    afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    it("should fetch empty list of groups", function() {
        // given
        $httpBackend.when('GET', hermesUrl('/groups')).respond([]);
        $httpBackend.when('GET', hermesUrl('/topics')).respond([]);

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
        $httpBackend.when('GET', hermesUrl('/groups')).respond(['g1', 'g2', 'g3', 'g12']);
        $httpBackend.when('GET', hermesUrl('/topics')).respond(['g1.t1', 'g1.t2', 'g3.t1', 'g12.t1']);


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
