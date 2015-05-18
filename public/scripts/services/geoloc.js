angular.module('claudeApp').factory('GeolocFactory', ['$rootScope', '$http', '$timeout',
    function ($rootScope, $http, $timeout) {
        $rootScope.geoLoc = '';
        if ($rootScope.geoLoc == '') {
            $http.get('/users/geographicPoint/ ').success(function (data) {
                if (data.status != 'fail') {
                    $rootScope.geoLoc = data;
                } else {
                    $rootScope.geoLoc = '(45.768434199999994,4.8153293999999995)'
                }
            })
        }
        function getPos(position) {
            $timeout(function () {
                $rootScope.$apply(function () {
                    $rootScope.geoLoc = "(" + position.coords.latitude + "," + position.coords.longitude + ")";
                    return $rootScope.geoLoc;
                });

            },0)
        }

        function erreurPosition(error) {
            $http.get('/users/geographicPoint/ ').success(function (data) {
                if (data.status != 'fail') {
                    $rootScope.geoLoc = data;
                    return $rootScope.geoLoc;
                }
            })
        }

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(getPos, erreurPosition, { enableHighAccuracy: false, timeout: 1000});
        } else {
            $http.get('/users/geographicPoint/ ').success(function (data) {
                if (data.status != 'fail') {
                    $rootScope.geoLoc = data;
                    return $rootScope.geoLoc;
                }
            })
        }
        return $rootScope.geoLoc;
}]);