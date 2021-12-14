describe("AvroNodeDirective", function () {

    var $compile;
    var $rootScope;

    beforeEach(module('templates'));
    beforeEach(module('hermes.topic.avroViewer'));

    beforeEach(inject(function (_$compile_, _$rootScope_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
    }));

    it("should properly render a simple field", function () {
        // given
        const element = $compile("<avro-node field='field'></avro-node>")($rootScope);
        const simpleField = {
            "name": "test",
            "type": "string",
            "doc": "Test field.",
            "default": "test"
        };

        // when
        $rootScope.field = simpleField;
        $rootScope.$digest();

        // then
        expect($(element).find("[data-ref='name']").text()).toBe("test:");
        expect($(element).find("[data-ref='type']").text()).toBe("string");
        expect($(element).find("[data-ref='doc']").text()).toBe("Test field.");
        expect($(element).find("[data-ref='default']").text()).toBe("Default: test");
    });

    it("should properly render a root field", function () {
        // given
        const element = $compile("<avro-node root='true' field='field'></avro-node>")($rootScope);
        const simpleField = {
            "name": "test",
            "type": {"type": "record", "fields": []},
            "doc": "Test field."
        };

        // when
        $rootScope.field = simpleField;
        $rootScope.$digest();

        // then
        expect($(element).find("[data-ref='name']").text()).toBe("test");
        expect($(element).find("[data-ref='type']").length).toBe(0);
        expect($(element).find("[data-ref='doc']").text()).toBe("Test field.");
    });
});
