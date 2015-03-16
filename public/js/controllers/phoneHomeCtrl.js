app.controller('phoneHomeCtrl', function ($scope, $rootScope, $http) {
    $scope.events = [];
    var offset = 0;
    $scope.goTo = function (id) {
        console.log(id)
        //window.location.href =('#/event/' + id);
    }
    function getEvents () {
        $scope.map = false;
        $http.get('/events/offset/' + offset + '/' + $rootScope.geoLoc).
            success(function (data, status, headers, config) {
                $scope.events = data;
                var eventsLength = $scope.events.length;
                for (var i = 0; i < eventsLength; i++) {
                    if ( $scope.events[i].addresses[0] != undefined) {
                        $scope.events[i].addresses[0].geographicPoint = $scope.events[i].addresses[0].geographicPoint.replace('(', '').replace(')', '');
                    }
                }
                $scope.events = [data[1]]
                $scope.map = true;
                console.log(data)
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