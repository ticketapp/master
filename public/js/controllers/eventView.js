app.controller ('EventViewCtrl',['$scope', '$routeParams', '$http', '$rootScope', '$timeout',
    function ($scope, $routeParams, $http, $rootScope, $timeout ){
    $scope.map = false;
    $scope.adresses = false;
    $scope.zoom = 12;
    $scope.travelMode = 'DRIVE';
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
    $http.get('/events/' + $routeParams.id)
        .success(function(data, status){
            $rootScope.marginContent();
            if (data.places.length > 0) {
                data.addresses[0].geographicPoint = data.places[0].geographicPoint.replace("(", "");
                data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace(")", "");
                data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace(",", ", ");
                $scope.adresses = true;
                console.log($scope.adresses);
            }
            if (data.tariffRange != undefined) {
                var tariffs = data.tariffRange.split('-');
                console.log(tariffs)
                if (tariffs[1] > tariffs[0]) {
                    data.tariffRange = tariffs[0] + '€ - ' + tariffs[1] + '€';
                } else {
                    data.tariffRange = tariffs[0] + '€';
                }
            }
            if (data.ticketSellers != undefined) {
                if (data.ticketSellers.indexOf("digitick") > -1) {
                    data.ticketPlatform = "digitick";
                }
                if (data.ticketSellers.indexOf("weezevent") > -1) {
                    data.ticketPlatform = "weezevent";
                }
                if (data.ticketSellers.indexOf("yurplan") > -1) {
                    data.ticketPlatform = "yurplan";
                }
                if (data.ticketSellers.indexOf("eventbrite") > -1) {
                    data.ticketPlatform = "eventbrite";
                }
                if (data.ticketSellers.indexOf("ticketmaster") > -1) {
                    data.ticketPlatform = "ticketmaster";
                }
                if (data.ticketSellers.indexOf("ticketnet") > -1) {
                    data.ticketPlatform = "ticketnet";
                }
            }
            $scope.event = data;
            console.log($scope.event);
            if ( $rootScope.window != 'small' && $rootScope.window != 'medium') {
                var waitForBinding = setInterval(function () {
                    if (document.getElementById('eventDescBind').innerHTML.length > 0) {
                        clearInterval(waitForBinding);
                        var eventInfoConteners = document.getElementsByClassName('eventInfo');
                        for (var i = 0; i < eventInfoConteners.length; i++) {
                            if (eventInfoConteners[i].offsetLeft < 30) {
                                eventInfoConteners[i].classList.remove('large-4');
                                eventInfoConteners[i].classList.add('large-12');
                            }
                        }
                    } else {
                        clearInterval(waitForBinding);
                        var eventInfoConteners = document.getElementsByClassName('eventInfo');
                        for (var i = 0; i < eventInfoConteners.length; i++) {
                            if (eventInfoConteners[i].offsetLeft < 30) {
                                eventInfoConteners[i].classList.remove('large-4');
                                eventInfoConteners[i].classList.add('large-12');
                            }
                        }
                    }
                    $scope.map = true;
                    $scope.$apply();
                }, 100);
            } else {
                $scope.map = true;
            }
        }).error(function(data, status){
            console.log(data);
        });
        $scope.follow = function (id) {
            $http.post('/events/'+ id + '/follow').
                success(function (data) {
                    alert('vous suivez maintenant ' + $scope.event.name)
                }).
                error(function (data) {
                    if (data.error == 'Credentials required') {
                        $rootScope.storeLastReq('post', '/events/'+ id + '/follow', '', 'vous suivez maintenant ' + $scope.event.name)
                    }
                    console.log(data)
                })
        };
}]);
