app.controller ('UsersCtrl', function ($scope, UserFactory, $routeParams, $http, $rootScope){
    $scope.map = false;
    $scope.heightMap = 300;
    console.log($routeParams);
    $http.get('/organizers/' + $routeParams.id +'/events')
        .success(function(data, status){
            $scope.orgaEvents = data;
            $scope.$apply();
            $scope.$digest();
            $rootScope.resizeImgHeight();
            if ($scope.orgaEvents.length > 0 && $rootScope.window != 'small' && $rootScope.window != 'medium') {
                angular.element(document).ready(function(){
                    setTimeout(function() {
                        var eventInfoConteners = document.getElementsByClassName('eventInfo');
                        var eventsContener = document.getElementsByClassName('data-ng-event');
                        var indexEventRef =(Math.ceil(
                            eventInfoConteners[0].clientHeight / eventsContener[0].clientHeight)*2);
                        if (indexEventRef >= $scope.orgaEvents.length -1) {
                            $scope.largeMap = true;
                            $scope.$apply();
                            eventInfoConteners = document.getElementsByClassName('eventInfo');
                            if (indexEventRef -1 < eventsContener.length -1 && indexEventRef -1 > 0) {
                                var lastEventPos = eventsContener[indexEventRef -1].getBoundingClientRect();
                                console.log(lastEventPos)
                            } else {
                                var lastEventPos = eventsContener[eventsContener.length - 1].getBoundingClientRect();
                                console.log(lastEventPos)
                            }
                            var descPos = eventInfoConteners[0].getBoundingClientRect();
                            console.log(descPos)
                            var newHeight = (lastEventPos.bottom - descPos.bottom) - 10;
                            console.log(newHeight)
                            if (newHeight < (eventsContener[0].clientHeight)/3) {
                                if (newHeight > 0) {
                                    eventInfoConteners[0].style.paddingBottom = newHeight + 'px';
                                }
                                console.log(newHeight)
                                console.log(eventsContener[0].clientHeight/3);
                                if (newHeight < -(eventsContener[0].clientHeight/3)) {
                                    $scope.medMap = true;
                                    $scope.descHeight = eventsContener[0].clientHeight + 290;
                                } else if (newHeight < (eventsContener[0].clientHeight)/3) {
                                    if (eventsContener.length == 1 || eventsContener.length %2 == 0) {
                                        $scope.fullMap = true;
                                    }
                                    $scope.heightMap = eventsContener[0].clientHeight - 30 +'px';
                                } else {
                                    descPos = eventInfoConteners[1].getBoundingClientRect();
                                    lastEventPos = eventsContener[eventsContener.length -1].getBoundingClientRect();
                                    newHeight = (lastEventPos.bottom - descPos.bottom) - 10;
                                    $scope.heightMap = newHeight + 'px';
                                }
                            } else {
                                descPos = eventInfoConteners[1].getBoundingClientRect();
                                lastEventPos = eventsContener[eventsContener.length -1].getBoundingClientRect();
                                newHeight = (lastEventPos.bottom - descPos.bottom) - 10;
                             $scope.heightMap = newHeight + 'px';
                            }
                        } else {
                            $scope.largeMap = false;
                            var lastEventPos;
                            if (indexEventRef <= 0) {
                                lastEventPos = eventsContener[0].getBoundingClientRect()
                            } else {
                                lastEventPos = eventsContener[indexEventRef -1].getBoundingClientRect();
                            }
                            console.log(indexEventRef)
                            var descPos = eventInfoConteners[0].getBoundingClientRect();
                            var newHeight = (lastEventPos.bottom - descPos.bottom) - 9;
                            console.log(newHeight)
                            console.log(eventsContener[0].clientHeight/3)
                            if (newHeight < (eventsContener[0].clientHeight)/3) {
                                newHeight = newHeight + eventsContener[0].clientHeight;
                                console.log(newHeight);
                                console.log(eventsContener[0])
                            }
                            $scope.heightMap = newHeight + 'px';
                            //$scope.heightMap = lastEventPos.bottom - descPos.bottom;
                        }
                        $scope.map = true;
                        $scope.$apply();
                    }, 500)
                })
            } else {
                $scope.map = true;
                $scope.largeMap = true;
                $scope.fullMap = true;
                $rootScope.$watch('window', function(newval) {
                    if (newval == 'large' || newval == 'xlarge' || newval == 'xxlarge') {
                        var waitForBinding = setInterval(function () {
                            if (document.getElementById('events_contener').innerHTML.length > 0) {
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
                        }, 100);
                    }
                })
            }
        });
    $http.get('/organizers/' + $routeParams.id)
        .success(function(data, status){
            $scope.organizer = data;
        }).error(function(data, status){
        });
});
