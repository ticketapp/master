angular.module('claudeApp').factory('GeolocFactory', ['$rootScope', '$http', '$timeout', '$localStorage', '$q',
    function ($rootScope, $http, $timeout, $localStorage, $q) {
        var factory = {
            geolocation: {
                geolocation: false,
                time: new Date()
            },
            getPositionByIp: function() {
                var deferred = $q.defer();
                $http.get('/users/geographicPoint').success(function (data) {
                    if (data.status !== 'fail') {
                        factory.geolocation.geolocation = data.lat + "," + data.lon;
                        factory.geolocation.time = new Date();
                        $localStorage.geoloc = factory.geolocation.geolocation;
                        deferred.resolve(factory.geolocation.geolocation)
                    }
                });
                return deferred.promise;
            },
            getPosistionByHtml5: function(position) {
                factory.geolocation.geolocation = position.coords.latitude + "," + position.coords.longitude;
                factory.geolocation.time = new Date();
                $localStorage.geoloc = factory.geolocation.geolocation;
                return factory.geolocation.geolocation
            },
            getGeolocation : function() {
                if ($localStorage.geoloc === undefined) {
                    $localStorage.geoloc = '45.768434199999994,4.8153293999999995';
                    factory.geolocation.geolocation = $localStorage.geoloc;
                    factory.geolocation.time = new Date();
                } else {
                    factory.geolocation.geolocation = $localStorage.geoloc;
                    factory.geolocation.time = new Date();
                }
                var deferred = $q.defer();
                if (factory.geolocation.geolocation !== false && (new Date() - factory.geolocation.time) > 360000) {
                    deferred.resolve(factory.geolocation.geolocation)
                } else {
                    if (navigator.geolocation) {
                        navigator.geolocation.getCurrentPosition(
                            factory.getPosistionByHtml5, factory.getPositionByIp,
                            { enableHighAccuracy: false, timeout: 1000}
                        );
                        deferred.resolve(factory.geolocation.geolocation)
                    } else {
                        factory.getPositionByIp.then(function(geoloc) {
                            deferred.resolve(geoloc)
                        })
                    }
                }
                return deferred.promise;
            }
        };
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
            $http.get('/users/geographicPoint').success(function (data) {
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
            $http.get('/users/geographicPoint').success(function (data) {
                if (data.status !== 'fail') {
                    $rootScope.geoLoc = data.lat + "," + data.lon;
                    $localStorage.geoloc = $rootScope.geoLoc;
                    return $rootScope.geoLoc;
                }
            })
        }
        if ($rootScope.geoLoc === '') {
            $http.get('/users/geographicPoint').success(function (data) {
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
        return factory;
}]);