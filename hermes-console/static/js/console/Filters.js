var filters = angular.module('hermes.filters', []);

filters.filter('readableSize', function () {
    return function (size) {
        if (size) {
            var i = Math.floor(Math.log(size)/Math.log(1024));
            return (size / Math.pow(1024, i)).toFixed(2) * 1 + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
        }
        return "";
    };
});

filters.filter('prettyJson', function () {

    return function (jsonString) {
        if (jsonString) {
            return JSON.stringify(JSON.parse(jsonString), null, 2);
        }
        return "null";
    };
});

filters.filter('toLowercase', function () {
    return function(string) {
        return angular.lowercase(string);
    };
});