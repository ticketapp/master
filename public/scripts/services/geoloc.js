angular.module('claudeApp').factory('GeolocFactory', ['$rootScope', '$http',
    function ($rootScope, $http) {
    $rootScope.geoLoc = '';
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (position) {
                $rootScope.geoLoc = "(" + position.coords.latitude + "," + position.coords.longitude + ")";
                $rootScope.$apply();
            }, function erreurPosition(error) {
                $http.get('/users/geographicPoint/ ').success(function (data) {
                    $rootScope.geoLoc = data;
                })
            }
        );
    } else {
        $http.get('/users/geographicPoint/ ').success(function (data) {
            $rootScope.geoLoc = data;
        })
    }
    return $rootScope.geoLoc;
}]);