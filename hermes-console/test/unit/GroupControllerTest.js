describe("GroupController", function() {

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

    it("should fetch group with all properties", function() {

        // given
        var group = {
            "groupName": "groupWithContact",
            "groupPassword": "*****",
            "technicalOwner": "Owner",
            "supportTeam": "Example Team",
            "contact": "fake-contact-xyz@zyz.allegro.pl"
        };

        $httpBackend.when('GET', hermesUrl('/groups/groupWithContact')).respond(group);

        $httpBackend.when('GET', hermesUrl('/groups')).respond(['groupWithContact']);
        $httpBackend.when('GET', hermesUrl('/topics')).respond([]);


        // when
        var $scope = {};
        var $stateParams = {groupName: "groupWithContact"};
        $controller('GroupController', {$scope: $scope, $stateParams: $stateParams});
        $httpBackend.flush();

        // then
        expect($scope.group.groupName).toEqual(group.groupName);
        expect($scope.group.technicalOwner).toEqual(group.technicalOwner);
        expect($scope.group.supportTeam).toEqual(group.supportTeam);
        expect($scope.group.contact).toEqual(group.contact);
        expect($scope.fetching).toEqual(false);

    });

});
