angular.module('claudeApp').factory('GenresFactory', ['$q', '$http', 'RoutesFactory',
    function($q, $http, RoutesFactory) {
        var factory = {
            isAGenre: function(genre) {
                var deferred = $q.defer();

                $http.get(RoutesFactory.genres.isAGenre(genre)).success(function(success) {
                    deferred.resolve(success)
                }).error(function (error) {
                    deferred.reject(error)
                });

                return deferred.promise;
            }
        };
        return factory;
    }]);