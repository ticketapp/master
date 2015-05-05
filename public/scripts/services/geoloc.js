angular.module('claudeApp').factory('GeolocFactory', ['$rootScope', function ($rootScope) {
    $rootScope.geoLoc = '';
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function (position) {
                $rootScope.geoLoc = "(" + position.coords.latitude + "," + position.coords.longitude + ")";
                $rootScope.$apply();
            }, function erreurPosition(error) {
            }
        );
    }
    return $rootScope.geoLoc;
}]);