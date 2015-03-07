app.controller ('UsersCtrl', function ($scope, UserFactory, $routeParams, $http, $rootScope){
    $http.get('/organizers/' + $routeParams.id +'/events')
        .success(function(data, status){
            $scope.orgaEvents = data;
            $rootScope.resizeImgHeight();
            console.log($scope.orgaEvents);
            if ($scope.orgaEvents.length > 0 && $rootScope.window != 'small' && $rootScope.window != 'medium') {
                var waitForBinding = setInterval(function () {
                    if (document.getElementById('events_contener').innerHTML.length > 0) {
                        clearInterval(waitForBinding);
                        var eventInfoConteners = document.getElementsByClassName('eventInfo');
                        if ($scope.orgaEvents.length == 1) {
                            document.getElementsByClassName('descriptionContent')[0].classList.remove('large-8');
                            document.getElementsByClassName('descriptionContent')[0].classList.add('large-4');
                            document.getElementsByClassName('descriptionContent')[0].classList.add('paddingLeft0');
                            document.getElementsByClassName('data-ng-event')[0].classList.add('width100p');
                            document.getElementsByClassName('min_contener')[0].classList.add('padding0');
                            $rootScope.resizeImgHeight();
                            var descPlace = document.getElementsByClassName('descriptionContent')[0].getBoundingClientRect();
                            for (var i = 0; i < eventInfoConteners.length; i++) {
                                eventInfoConteners[i].classList.remove('large-4');
                                eventInfoConteners[i].classList.add('large-8');
                                var infoPlace = eventInfoConteners[i].getBoundingClientRect();
                                if (infoPlace.top > descPlace.bottom) {
                                    eventInfoConteners[i].classList.remove('large-8');
                                    eventInfoConteners[i].classList.add('large-12');
                                }
                            }
                        }
                        setTimeout(function(){
                        for (var i = 0; i < eventInfoConteners.length; i++) {
                            if (eventInfoConteners[i].offsetLeft < 30) {
                                eventInfoConteners[i].classList.remove('large-4');
                                eventInfoConteners[i].classList.remove('large-8');
                                eventInfoConteners[i].classList.add('large-12');
                            }
                        }
                        },100);
                    }
                }, 100);
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
            console.log(data)
        }).error(function(data, status){
        });
});
