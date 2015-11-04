angular.module('claudeApp').factory('GeolocFactory', ['$rootScope', '$http', '$timeout', '$localStorage',
    function ($rootScope, $http, $timeout, $localStorage) {
        $rootScope.geoLoc = '';

        if ($localStorage.geoloc === undefined) {
            $localStorage.geoloc = '45.768434199999994,4.8153293999999995';
        }
        function getPos(position) {
            console.log(position);
            $timeout(function () {
                $rootScope.$apply(function () {
                    $rootScope.geoLoc = position.coords.latitude + "," + position.coords.longitude;
                    $localStorage.geoloc = $rootScope.geoLoc;
                    return $rootScope.geoLoc;
                });

            },0)
        }

        function errorPosition(error) {
            $http.get('/users/geographicPoint/ ').success(function (data) {
                if (data.status !== 'fail') {
                    $rootScope.geoLoc = data.lat + "," + data.lon;
                    $localStorage.geoloc = data;
                    return $rootScope.geoLoc;
                }
            })
        }

        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(getPos, errorPosition, { enableHighAccuracy: false, timeout: 1000});
        } else {
            $http.get('/users/geographicPoint/ ').success(function (data) {
                if (data.status !== 'fail') {
                    $rootScope.geoLoc = data.lat + "," + data.lon;
                    $localStorage.geoloc = $rootScope.geoLoc;
                    return $rootScope.geoLoc;
                }
            })
        }
        if ($rootScope.geoLoc === '') {
            $http.get('/users/geographicPoint/ ').success(function (data) {
                if (data.status !== 'fail') {
                    $rootScope.geoLoc = data.lat + "," + data.lon;
                    $localStorage.geoloc = $rootScope.geoLoc;
                } else {
                    $rootScope.geoLoc = $localStorage.geoloc
                }
            }).error(function(error) {
                $rootScope.geoLoc = $localStorage.geoloc
            })
        }
        return $rootScope.geoLoc;
}]);