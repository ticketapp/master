angular.module('claudeApp').factory('GeolocFactory', ['$rootScope', '$http', '$timeout', '$localStorage',
    function ($rootScope, $http, $timeout, $localStorage) {
        $rootScope.geoLoc = '';
        if ($localStorage.geoloc == undefined) {
            $localStorage.geoloc = '(45.768434199999994,4.8153293999999995)';
        }
        if ($rootScope.geoLoc == '') {
            $http.get('/users/geographicPoint/ ').success(function (data) {
                if (data.status != 'fail') {
                    $rootScope.geoLoc = data;
                    $localStorage.geoloc = data;
                } else {
                    $rootScope.geoLoc = $localStorage.geoloc
                }
            })
        }
        function getPos(position) {
            $timeout(function () {
                $rootScope.$apply(function () {
                    $rootScope.geoLoc = "(" + position.coords.latitude + "," + position.coords.longitude + ")";
                    $localStorage.geoloc = $rootScope.geoLoc;
                    return $rootScope.geoLoc;
                });

            },0)
        }

        function erreurPosition(error) {
            $http.get('/users/geographicPoint/ ').success(function (data) {
                if (data.status != 'fail') {
                    $rootScope.geoLoc = data;
                    $localStorage.geoloc = data;
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
                    $localStorage.geoloc = data;
                    return $rootScope.geoLoc;
                }
            })
        }
        return $rootScope.geoLoc;
}]);