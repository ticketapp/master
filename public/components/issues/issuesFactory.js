angular.module('claudeApp').factory('IssuesFactory', ['$http', '$q',
    function ($http, $q) {
        var factory = {
            issues : false,
            getIssues : function () {
                var deferred = $q.defer();
                $http.get('/issues').
                    success(function (data) {
                        factory.issues = data;
                        deferred.resolve(factory.issues)
                    });
                return deferred.promise;
            },
            getIssueComments : function (id) {
                var deferred = $q.defer();
                $http.get('/issues/'+ id + '/comments ').
                    success(function (data) {
                        factory.issues = data;
                        deferred.resolve(factory.issues)
                    }).
                    error(function (data) {

                    });
                return deferred.promise;
            }
        };
        return factory;
}]);