var avroViewer = angular.module('hermes.topic.avroViewer', []);

avroViewer.directive('avroViewer', function () {
    return {
        template: '<avro-node ng-if="rootField" root="true" field="rootField"></avro-node>',
        restrict: 'E',
        scope: {
            schema: '='
        },
        controller: function ($scope) {
            $scope.$watch('schema', (value) => value && updateFields());

            function updateFields() {
                const jsonSchema = JSON.parse($scope.schema);
                $scope.rootField = {
                    name: jsonSchema.name,
                    doc: jsonSchema.doc,
                    type: {
                        type: "record",
                        fields: jsonSchema.fields.filter(field => field.name !== '__metadata')
                    }
                };
            }
        }
    };
});

avroViewer.controller('AvroNodeController', ['$scope', function ($scope) {
    $scope.name = $scope.root ? $scope.field.name : `${$scope.field.name}:`;
    $scope.doc = $scope.field.doc && formatDocumentation($scope.field.doc);
    $scope.types = getTypes($scope.field.type);
    $scope.isRecord = $scope.types.some(type => type.includes('record'));
    $scope.isEnum = $scope.types.some(type => type.includes('enum'));
    $scope.expandable = $scope.isRecord || $scope.isEnum;
    $scope.expanded = true;

    const FOLD_EVENT = "fold";
    $scope.toggleExpansion = function () {
        $scope.expanded = !$scope.expanded;
        if ($scope.expanded === false) {
            $scope.$broadcast(FOLD_EVENT);
        }
    };
    $scope.$on(FOLD_EVENT, () => $scope.expanded = false);

    if ($scope.isRecord) {
        $scope.nestedType = findNestedType($scope.field.type);
    }

    if ($scope.isEnum) {
        $scope.enumSymbols = findEnumSymbols($scope.field.type);
    }

    if ($scope.expandable) {
        $scope.expandableTypesNumber = countExpandableTypes($scope.types);
    }

    function getTypes(typeField) {
        if (typeof typeField === 'string' || typeField instanceof String) {
            return [typeField.toLowerCase()];
        } else if (Array.isArray(typeField)) {
            let types = typeField.flatMap(type => getTypes(type));
            const nullable = types.includes('null');
            if (nullable) {
                types = types.filter(type => type !== 'null').map(type => `${type}?`);
            }
            return types;
        } else if (typeField.hasOwnProperty('type')) {
            return [getComplexType(typeField)];
        }
        return ['unknown'];
    }

    function getComplexType(type) {
        if (type.type === 'array') {
            const arrayType = getTypes(type.items)[0];
            return `[]${arrayType}`;
        } else if (type.type === 'map') {
            const mapType = getTypes(type.values)[0];
            return `map[string]${mapType}`;
        } else if (type.type === 'fixed') {
            return `fixed(${type.size}B)`;
        }
        return type.type;
    }

    function findNestedType(type) {
        if (Array.isArray(type)) {
            return type
                .map(subType => findNestedType(subType))
                .find(subType => subType.hasOwnProperty('type'));
        }
        if (type.type === 'array') {
            return type.items;
        }
        if (type.type === 'map') {
            return type.values;
        }
        return type;
    }

    function findEnumSymbols(type) {
        if (Array.isArray(type)) {
            return type.find(subType => subType.hasOwnProperty('symbols')).symbols;
        }
        if (type.type === 'enum') {
            return type.symbols;
        }
        if (type.type === 'array') {
            return type.items.symbols;
        }
        if (type.type === 'map') {
            return type.values.symbols;
        }
    }

    function countExpandableTypes(types) {
        return types
            .filter(subType => subType.includes("record") || subType.includes("enum"))
            .length;
    }

    function formatDocumentation(documentation) {
        let formattedDocumentation = documentation.trim();
        if(formattedDocumentation.length === 0) {
            return null;
        }
        formattedDocumentation = formattedDocumentation[0].toUpperCase() + formattedDocumentation.slice(1);
        if(formattedDocumentation.slice(-1).match(/[a-zA-Z0-9]/i)) {
            formattedDocumentation += '.';
        }
        return formattedDocumentation;
    }
}]);

avroViewer.directive('avroNode', ['$compile', function ($compile) {
    return {
        templateUrl: 'partials/avroViewerNode.html',
        restrict: 'E',
        scope: {
            field: '=',
            root: '=?'
        },
        controller: 'AvroNodeController',
        link: function postLink(scope, element) {
            let template;
            if (scope.isRecord) {
                template = `<avro-node ng-repeat="nestedField in nestedType.fields track by nestedField.name"
                            field="nestedField"></avro-node>`;
            } else if (scope.isEnum) {
                template = `<div class="tree-branch"></div>
                            <span ng-repeat="symbol in enumSymbols">
                                <code>{{symbol}}</code>
                                <span ng-if="!$last">, </span>
                            </span>`;
            }
            const newElement = $compile(template)(scope);
            element.find('[data-ref="children"]').html(newElement);
        }
    };
}]);
