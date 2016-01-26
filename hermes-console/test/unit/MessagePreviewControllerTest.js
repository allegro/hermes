describe("MessagePreviewController", function() {

    beforeEach(module('hermes.messagePreview'));

    var $controller;

    beforeEach(inject(function(_$controller_) {
        $controller = _$controller_;
    }));

    it("set up all necessary $scope properties", function() {
        var $scope = {};

        $controller('MessagePreviewController', { $scope: $scope,
                                                  topicName: 'exampleTopicName',
                                                  partition: 778,
                                                  offset: 98798798,
                                                  previewedMessage: 'This is example message' });

        expect($scope.topicName).toBe('exampleTopicName');
        expect($scope.partition).toBe(778);
        expect($scope.offset).toBe(98798798);
        expect($scope.previewedMessage).toBe('This is example message');
    });
});
