app.controller ('UsersCtrl', function ($scope, UserFactory, $routeParams, $http, $rootScope, $location){
    $scope.map = false;
    $scope.heightMap = 300;
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
            }
        }).error(function(data, status){
        });
    $http.get('/' + $scope.getUrl + '/' + $routeParams.id +'/events')
        .success(function(data, status){
            $scope.orgaEvents = data;
            $rootScope.resizeImgHeight();
            if ($scope.orgaEvents.length > 0 && $rootScope.window != 'small' && $rootScope.window != 'medium') {
                angular.element(document).ready(function(){
                    var waitEvents = setInterval(function() {
                        var eventInfoConteners = document.getElementsByClassName('eventInfo')
                        var eventsContener = document.getElementsByClassName('data-ng-event');
                        console.log(eventsContener.length)
                        if (eventsContener.length == $scope.orgaEvents.length) {
                            console.log('yo')
                            clearInterval(waitEvents);
                            var indexEventRef = (Math.ceil(
                                    eventInfoConteners[0].clientHeight / eventsContener[0].clientHeight) * 2);
                            if (indexEventRef >= $scope.orgaEvents.length - 1 && eventInfoConteners[0].clientHeight > (eventsContener[0].clientHeight/3)*2) {
                                if ($scope.organizer.geographicPoint != undefined) {
                                    console.log(1)
                                    $scope.largeMap = true;
                                    $scope.$apply();
                                }
                                eventInfoConteners = document.getElementsByClassName('eventInfo');
                                if (indexEventRef - 1 < eventsContener.length - 1 && indexEventRef - 1 > 0) {
                                    var lastEventPos = eventsContener[indexEventRef - 1].getBoundingClientRect();
                                    console.log(lastEventPos)
                                } else {
                                    var lastEventPos = eventsContener[eventsContener.length - 1].getBoundingClientRect();
                                    console.log(lastEventPos)
                                }
                                var descPos = eventInfoConteners[0].getBoundingClientRect();
                                console.log(descPos)
                                var newHeight = (lastEventPos.bottom - descPos.bottom) - 10;
                                console.log(newHeight)
                                if (newHeight < (eventsContener[0].clientHeight) / 3) {
                                    if (newHeight > 0) {
                                        eventInfoConteners[0].style.paddingBottom = newHeight + 'px';
                                    }
                                    console.log(newHeight)
                                    console.log(eventsContener[0].clientHeight / 3);
                                    if (newHeight < -(eventsContener[0].clientHeight / 3) && $scope.orgaEvents.length > 1) {
                                        $scope.medMap = true;
                                        if ($scope.organizer.geographicPoint != undefined) {
                                            $scope.descHeight = eventsContener[0].clientHeight + 290;
                                        } else {
                                            $scope.descHeight = eventsContener[0].clientHeight
                                        }
                                    } else if (newHeight < (eventsContener[0].clientHeight) / 3) {
                                        if (eventsContener.length == 1 || eventsContener.length % 2 == 0) {
                                            $scope.fullMap = true;
                                            if (newHeight < -(eventsContener[0].clientHeight / 3)) {
                                                $scope.descHeight = eventsContener[0].clientHeight - 25;
                                            }
                                        }
                                        $scope.heightMap = eventsContener[0].clientHeight + 'px';
                                    } else {
                                        descPos = eventInfoConteners[1].getBoundingClientRect();
                                        lastEventPos = eventsContener[eventsContener.length - 1].getBoundingClientRect();
                                        newHeight = (lastEventPos.bottom - descPos.bottom);
                                        $scope.heightMap = newHeight + 'px';
                                    }
                                } else {
                                    descPos = eventInfoConteners[1].getBoundingClientRect();
                                    lastEventPos = eventsContener[eventsContener.length - 1].getBoundingClientRect();
                                    newHeight = (lastEventPos.bottom - descPos.bottom);
                                    $scope.heightMap = newHeight + 'px';
                                }
                            } else {
                                $scope.largeMap = false;
                                var lastEventPos;
                                console.log(indexEventRef)
                                if (indexEventRef <= 0 || eventsContener.length == 1) {
                                    lastEventPos = eventsContener[0].getBoundingClientRect()
                                } else {
                                    lastEventPos = eventsContener[indexEventRef - 1].getBoundingClientRect();
                                }
                                var descPos = eventInfoConteners[0].getBoundingClientRect();
                                var newHeight = (lastEventPos.bottom - descPos.bottom) - 9;
                                console.log(newHeight)
                                console.log(eventsContener[0].clientHeight / 3)
                                if (newHeight < (eventsContener[0].clientHeight) / 3) {
                                    newHeight = newHeight + eventsContener[0].clientHeight;
                                    console.log(newHeight);
                                    console.log(eventsContener[0])
                                }
                                if ($scope.organizer.geographicPoint == undefined && $scope.organizer.description != undefined) {
                                    eventInfoConteners[0].style.minHeight = eventsContener[0].clientHeight - 10 + 'px';
                                    console.log(eventsContener[0].clientHeight)
                                }
                                $scope.heightMap = newHeight + 'px';
                                //$scope.heightMap = lastEventPos.bottom - descPos.bottom;
                            }
                            if ($scope.organizer.geographicPoint != undefined) {
                                $scope.map = true;
                            }
                            $scope.$apply();
                        }
                    }, 500)
                });
            } else {
                if ($scope.organizer.geographicPoint != undefined) {
                    $scope.map = true;
                    $scope.largeMap = true;
                    $scope.fullMap = true;
                }
            }
        });
});
