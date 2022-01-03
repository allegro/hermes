describe("AvroViewerDirective", function () {

    const schema = `
    {
      "type": "record",
      "name": "TestSchema",
      "doc":  "Test schema doc",
      "namespace": "pl.allegro.test",
      "fields": [
        {
          "name": "__metadata",
          "type": {"type": "map", "values": "string"}
        },
        {
          "name": "field1",
          "type": "string"
        },
        {
          "name": "field2",
          "type": "int"
        }
      ]
    }`;

    var $compile;
    var $rootScope;

    beforeEach(module('templates'));
    beforeEach(module('hermes.topic.avroViewer'));

    beforeEach(inject(function (_$compile_, _$rootScope_) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
    }));

    it("should create dummy field for top-level node", function () {
        // given
        const element = $compile("<avro-viewer schema='schema'></avro-viewer>")($rootScope);

        // when
        $rootScope.schema = schema;
        $rootScope.$digest();

        // then
        const elementScope = element.isolateScope();
        expect(elementScope.rootField.name).toBe("TestSchema");
        expect(elementScope.rootField.doc).toBe("Test schema doc");
        expect(elementScope.rootField.type.type).toBe("record");
    });

    it("should add top level nodes of the schema except for __metadata", function () {
        // given
        const element = $compile("<avro-viewer schema='schema'></avro-viewer>")($rootScope);

        // when
        $rootScope.schema = schema;
        $rootScope.$digest();

        // then
        const elementScope = element.isolateScope();
        expect(elementScope.rootField.type.fields.length).toBe(2);
    });
});
