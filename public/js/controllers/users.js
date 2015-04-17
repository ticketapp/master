app.controller ('UsersCtrl', function ($scope, UserFactory, $routeParams, $http, $rootScope, $location, $timeout){
    $scope.map = false;
    $scope.zoom = 14;
    $scope.heightMap = '300px';
    $scope.travelMode = 'DRIVE';
    $scope.directionInfos ='';
    $scope.showDesc = false;
    $scope.moreZoom = function() {
        $scope.zoom = $scope.zoom + 1;
    };
    $scope.lessZoom = function() {
        $scope.zoom = $scope.zoom - 1;
    };
    $scope.getItineraire = function () {
        $scope.map = false;
        $scope.start = $rootScope.geoLoc.replace("(", "").replace(")", "").replace(",", ", ");
        $scope.map = true;
        var oldInf = window.directionInfos;
        var waitForInfosChanges = setInterval(function () {
            if (window.directionInfos != oldInf) {
                clearInterval(waitForInfosChanges)
                $timeout(function () {
                    $scope.$apply(function () {
                        $scope.directionInfos = window.directionInfos;
                    })
                }, 0)
            }
        }, 500);
    };
    $scope.changeTravelMode = function (travelMode) {
        $scope.map = false;
        $scope.travelMode = travelMode;
        $scope.map = true;
        var oldInf = window.directionInfos;
        var waitForInfosChanges = setInterval(function () {
            if (window.directionInfos != oldInf) {
                clearInterval(waitForInfosChanges)
                $timeout(function () {
                    $scope.$apply(function () {
                        $scope.directionInfos = window.directionInfos;
                    })
                }, 0)
            }
        }, 500);
    };
    if ($location.path().indexOf('lieu') > -1) {
        $scope.getUrl = 'places'
    } else {
        $scope.getUrl = 'organizers'
    }
    $http.get('/' + $scope.getUrl + '/' + $routeParams.id)
        .success(function(data, status){
            $scope.organizer = data;
            $rootScope.marginContent();
            console.log(data);
            if ($scope.organizer.geographicPoint != undefined) {
                $scope.organizer.geographicPoint = $scope.organizer.geographicPoint.replace("(", "");
                $scope.organizer.geographicPoint = $scope.organizer.geographicPoint.replace(")", "");
                $scope.organizer.geographicPoint = $scope.organizer.geographicPoint.replace(",", ", ");
                $scope.map = true;
            }
            $http.get('/' + $scope.getUrl + '/' + $routeParams.id +'/events')
                .success(function(data, status){
                    $scope.orgaEvents = [];
                    function pushEvent (el) {
                        el.priceColor = 'rgb(0, 140, 186)';
                        if (el.tariffRange != undefined) {
                            var tariffs = el.tariffRange.split('-');
                            if (tariffs[1] > tariffs[0]) {
                                el.tariffRange = tariffs[0].replace('.0', '') + '€ - ' +
                                    tariffs[1].replace('.0', '') + '€';
                            } else {
                                el.tariffRange = tariffs[0].replace('.0', '') + '€';
                            }
                            el.priceColor = 'rgb(' + tariffs[0]*2 + ',' + (200 - (tariffs[0]*4 ) )+
                                ',' + tariffs[0]*4 + ')'
                        }
                        $scope.orgaEvents.push(el);
                    }
                    data.forEach(pushEvent)
                    $rootScope.resizeImgHeight();
                });
        }).error(function(data, status){
        });
});
