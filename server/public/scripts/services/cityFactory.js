angular.module('claudeApp')
    .factory('CityFactory', ['$http', '$q', 'RoutesFactory', function ($http, $q, RoutesFactory) {
        var factory = {
            isACity : function(city) {
                var deferred = $q.defer();
                $http.get(RoutesFactory.city.isACity(city)).success(function(isACity) {
                    deferred.resolve(isACity)
                }).error(function(error) {
                    deferred.reject(error)
                });
                return deferred.promise
            }
        };
        return factory;
    }]);