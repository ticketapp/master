app.controller ('UsersCtrl', function ($scope, UserFactory, $routeParams, $http, $rootScope){
    $scope.map = false;
    $scope.heightMap = 300;
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
                            eventInfoConteners[0].clientHeight / eventsContener[0].clientHeight)*2)-1;
                        if (indexEventRef == $scope.orgaEvents.length -1) {
                            $scope.largeMap = true;
                        } else {
                            var lastEventPos = eventsContener[indexEventRef].getBoundingClientRect();
                            var descPos = eventInfoConteners[0].getBoundingClientRect();
                            var newHeight = (lastEventPos.bottom - descPos.bottom) - 10;
                            console.log(newHeight)
                            console.log(eventsContener[0].clientHeight/3)
                            if (newHeight < (eventsContener[0].clientHeight)/3) {
                                newHeight = newHeight + eventsContener[0].clientHeight;
                                console.log(newHeight);
                                console.log(eventsContener[0])
                            }
                            $scope.heightMap = newHeight + 'px';
                            //$scope.heightMap = lastEventPos.bottom - descPos.bottom;
                            $scope.map = true;
                            $scope.$apply();
                        }
                    }, 1000)
                })
            } else {
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
