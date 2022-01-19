describe("AvroNodeController", function () {

    var $controller;
    var $scope;

    beforeEach(module('hermes.topic.avroViewer'));

    beforeEach(inject(function ($rootScope, _$controller_) {
        $controller = _$controller_;
        $scope = $rootScope;
    }));

    [
        {
            name: "primitive",
            avroType: "string",
            expectedOutputType: ["string"]
        },
        {
            name: "nullable primitive",
            avroType: ["string", "null"],
            expectedOutputType: ["string?"]
        },
        {
            name: "record",
            avroType: {"type": "record"},
            expectedOutputType: ["record"]
        },
        {
            name: "nullable record",
            avroType: [{"type": "record"}, "null"],
            expectedOutputType: ["record?"]
        },
        {
            name: "primitive map",
            avroType: {"type": "map", "values": "int"},
            expectedOutputType: ["map[string]int"]
        },
        {
            name: "record map",
            avroType: {"type": "map", "values": {"type": "record"}},
            expectedOutputType: ["map[string]record"]
        },
        {
            name: "nullable map",
            avroType: [{"type": "map", "values": "int"}, "null"],
            expectedOutputType: ["map[string]int?"]
        },
        {
            name: "enum",
            avroType: {"type": "enum"},
            expectedOutputType: ["enum"]
        },
        {
            name: "primitive union",
            avroType: ["int", "string"],
            expectedOutputType: ["int", "string"]
        },
        {
            name: "nullable primitive union",
            avroType: ["int", "string", "null"],
            expectedOutputType: ["int?", "string?"]
        },
        {
            name: "primitive array",
            avroType: {"type": "array", "items": "int"},
            expectedOutputType: ["[]int"]
        },
        {
            name: "record array",
            avroType: {"type": "array", "items": {"type": "record"}},
            expectedOutputType: ["[]record"]
        },
        {
            name: "fixed",
            avroType: {"type": "fixed", "size": 16},
            expectedOutputType: ["fixed(16B)"]
        }
    ].forEach(testCase =>
        it(`should read ${testCase.name} type correctly`, function () {
            // given
            $scope.field = {
                type: testCase.avroType
            };

            // when
            $controller('AvroNodeController', {$scope: $scope});

            // then
            expect($scope.types).toEqual(testCase.expectedOutputType);
        })
    );

    it("should find record's children", function () {
        // given
        $scope.field = {
            "name": "test",
            "type": {
                "type": "record",
                "name": "testType",
                "fields": [
                    {
                        "name": "field1",
                        "type": "int",
                        "doc": "Field 1"
                    },
                    {
                        "name": "field2",
                        "type": "long",
                        "doc": "Field 2"
                    }
                ]
            }
        };

        // when
        $controller('AvroNodeController', {$scope: $scope});

        // then
        expect($scope.isRecord).toBeTruthy();
        expect($scope.expandable).toBeTruthy();
        expect($scope.nestedType.fields.length).toBe(2);
        expect($scope.nestedType.fields[0].name).toBe("field1");
        expect($scope.nestedType.fields[1].name).toBe("field2");
    });

    it("should find only children of the first record in union", function () {
        // given
        $scope.field = {
            "name": "test",
            "type": [
                {
                    "type": "record",
                    "name": "testType",
                    "fields": [{"name": "field1", "type": "int"}]
                },
                {
                    "type": "record",
                    "name": "testType",
                    "fields": [{"name": "field2", "type": "int"}]
                }
            ]
        };

        // when
        $controller('AvroNodeController', {$scope: $scope});

        // then
        expect($scope.isRecord).toBeTruthy();
        expect($scope.nestedType.fields.length).toBe(1);
        expect($scope.nestedType.fields[0].name).toBe("field1");
    });

    [
        {
            name: "enum",
            field: {
                "name": "test",
                "type": {
                    "type": "enum",
                    "name": "testType",
                    "symbols": ["one", "two", "three"]
                }
            }
        },
        {
            name: "enum array",
            field: {
                "name": "test",
                "type": {
                    "type": "array",
                    "items": {
                        "type": "enum",
                        "name": "testType",
                        "symbols": ["one", "two", "three"]
                    }
                }
            }
        },
        {
            name: "enum map",
            field: {
                "name": "test",
                "type": {
                    "type": "map",
                    "values": {
                        "type": "enum",
                        "name": "testType",
                        "symbols": ["one", "two", "three"]
                    }
                }
            }
        },
        {
            name: "enum union",
            field: {
                "name": "test",
                "type": [
                    {
                        "type": "enum",
                        "name": "testType",
                        "symbols": ["one", "two", "three"]
                    },
                    "int"
                ]
            }
        }
    ].forEach(testCase =>
        it(`should recognize ${testCase.name}`, function () {
            // given
            $scope.field = testCase.field;

            // when
            $controller('AvroNodeController', {$scope: $scope});

            // then
            expect($scope.isEnum).toBeTruthy();
            expect($scope.expandable).toBeTruthy();
            expect($scope.enumSymbols).toEqual(["one", "two", "three"]);
        })
    );

    it("should toggle expansion", function () {
        // given
        $scope.field = {
            "name": "test",
            "type": {
                "type": "record",
                "name": "testType",
                "fields": [{"name": "field1", "type": "int"}]
            }
        };

        // when
        $controller('AvroNodeController', {$scope: $scope});

        // then
        expect($scope.expanded).toBeTruthy();

        // when
        $scope.toggleExpansion();

        // then
        expect($scope.expanded).toBeFalsy();

        // when
        $scope.toggleExpansion();

        // then
        expect($scope.expanded).toBeTruthy();
    });

    it("should broadcast a fold event when collapsing a node", function () {
        // given
        $scope.field = {
            "name": "test",
            "type": {
                "type": "record",
                "name": "testType",
                "fields": [{"name": "field1", "type": "int"}]
            }
        };
        $controller('AvroNodeController', {$scope: $scope});
        spyOn($scope, '$broadcast');

        // and
        $scope.expanded = true;

        // when
        $scope.toggleExpansion();

        // then
        expect($scope.$broadcast).toHaveBeenCalledWith("fold");
    });

    it("should not broadcast an event when expanding a node", function () {
        // given
        $scope.field = {
            "name": "test",
            "type": {
                "type": "record",
                "name": "testType",
                "fields": [{"name": "field1", "type": "int"}]
            }
        };
        $controller('AvroNodeController', {$scope: $scope});
        spyOn($scope, '$broadcast');

        // and
        $scope.expanded = false;

        // when
        $scope.toggleExpansion();

        // then
        expect($scope.$broadcast).not.toHaveBeenCalled();
    });

    [
        {
            name: "union of primitive and record",
            field: {
                "name": "test",
                "type": [
                    {
                        "type": "record",
                        "name": "testType",
                        "fields": [{"name": "field1", "type": "int"}]
                    },
                    "int"
                ],
            },
            expectedCount: 1
        },
        {
            name: "union of primitive map and record",
            field: {
                "name": "test",
                "type": [
                    {
                        "type": "record",
                        "name": "testType",
                        "fields": [{"name": "field1", "type": "int"}]
                    },
                    {
                        "type": "map",
                        "name": "testType2",
                        "values": "int"
                    }
                ],
            },
            expectedCount: 1
        },
        {
            name: "union of records",
            field: {
                "name": "test",
                "type": [
                    {
                        "type": "record",
                        "name": "testType1",
                        "fields": [{"name": "field1", "type": "int"}]
                    },
                    {
                        "type": "record",
                        "name": "testType2",
                        "fields": [{"name": "field1", "type": "int"}]
                    }
                ],
            },
            expectedCount: 2
        },
        {
            name: "union of record and enum",
            field: {
                "name": "test",
                "type": [
                    {
                        "type": "record",
                        "name": "testType",
                        "fields": [{"name": "field1", "type": "int"}]
                    },
                    {
                        "type": "enum",
                        "name": "testType",
                        "symbols": ["one", "two", "three"]
                    }
                ]
            },
            expectedCount: 2
        }
    ].forEach(testCase =>
        it(`should count expandable types of ${testCase.name}`, function () {
            // given
            $scope.field = testCase.field;

            // when
            $controller('AvroNodeController', {$scope: $scope});

            // then
            expect($scope.expandableTypesNumber).toBe(testCase.expectedCount);
        })
    );

    it("should format a doc field", function () {
        // given
        $scope.field = {
            "name": "test",
            "type": "int",
            "doc": " some documentation  "
        };

        // when
        $controller('AvroNodeController', {$scope: $scope});

        // then
        expect($scope.doc).toBe("Some documentation.");
    });

    it("should not add a dot to documentation if there already is one", function () {
        // given
        $scope.field = {
            "name": "test",
            "type": "int",
            "doc": "some documentation."
        };

        // when
        $controller('AvroNodeController', {$scope: $scope});

        // then
        expect($scope.doc).toBe("Some documentation.");
    });

    it("should not add a dot to documentation if it ends with a non-letter character", function () {
        // given
        $scope.field = {
            "name": "test",
            "type": "int",
            "doc": "some documentation!"
        };

        // when
        $controller('AvroNodeController', {$scope: $scope});

        // then
        expect($scope.doc).toBe("Some documentation!");
    });
});
