app.controller('phoneHomeCtrl', function ($scope, $rootScope, $http) {
    $scope.events = [];
    $scope.infos = [];
    $scope.time = 6;
    var offset = 0;
    $scope.goTo = function (e, id) {
        id = $scope.events[id].eventId;
        window.location.href =('#/event/' + id);
    };
    $http.get('/infos').success(function (data, status, headers, config) {
        $scope.infos = data;
    });
    function getEvents () {
        $scope.map = false;
        $http.get('/events/offset/' + offset + '/' + $rootScope.geoLoc).
            success(function (data, status, headers, config) {
                $scope.events = data;
                var eventsLength = $scope.events.length;
                console.log(eventsLength)
                for (var i = 0; i < eventsLength; i++) {
                    if ( $scope.events[i].addresses[0] != undefined) {
                        $scope.events[i].addresses[0].geographicPoint = $scope.events[i].addresses[0].geographicPoint.replace('(', '').replace(')', '');
                    }
                    if ($scope.events[i].startTime != undefined) {
                        $scope.events[i].countdown = Math.round(($scope.events[i].startTime - new Date()) / 3600000);
                        if ($scope.time == 6 && $scope.events[i].countdown > $scope.time) {
                            $scope.time = $scope.events[i].countdown;
                        } else if ($scope.time != 6 && $scope.events[i].countdown < $scope.time && $scope.events[i].countdown > 6) {
                            $scope.time = $scope.events[i].countdown;
                        }
                    }
                }
                $scope.map = true;
            })
    }
    console.log($rootScope.geoLoc);
    if ($rootScope.geoLoc.length > 0) {
        getEvents ()
        $scope.mapCenter = $rootScope.geoLoc.replace('(', '');
        $scope.mapCenter = $scope.mapCenter.replace(')', '');
    } else {
        $rootScope.$watch('geoLoc', function () {
            getEvents ();
            $scope.mapCenter = $rootScope.geoLoc.replace('(', '');
            $scope.mapCenter = $scope.mapCenter.replace(')', '');
        })
    }
});