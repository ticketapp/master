app.controller ('UsersCtrl', function ($scope, UserFactory, $routeParams, $http, $rootScope){
    function imgHeight () {
        var waitForContentMin = setTimeout(function () {
            var content = document.getElementsByClassName('img_min_evnt');
            if (content.length > 0) {
                clearInterval(waitForContentMin);
                var newHeight = content[0].clientWidth * 0.376 + 'px';
                for (var i = 0; i < content.length; i++) {
                    content[i].style.height = newHeight;
                }
            }
        }, 100)
    }
    $http.get('/organizers/' + $routeParams.id +'/events')
        .success(function(data, status){
            $scope.orgaEvents = data;
            imgHeight();
            console.log($scope.orgaEvents);
            if ($scope.orgaEvents.length > 0 && $rootScope.window != 'small' && $rootScope.window != 'medium') {
                var waitForBinding = setInterval(function () {
                    if (document.getElementById('events_contener').innerHTML.length > 0) {
                        clearInterval(waitForBinding);
                        var eventInfoConteners = document.getElementsByClassName('eventInfo');
                        if ($scope.orgaEvents.length == 1) {
                            console.log('yo')
                            document.getElementsByClassName('descriptionContent')[0].classList.remove('large-8');
                            document.getElementsByClassName('descriptionContent')[0].classList.add('large-4');
                            document.getElementsByClassName('descriptionContent')[0].classList.add('paddingLeft0');
                            document.getElementsByClassName('data-ng-event')[0].classList.add('width100p');
                            document.getElementsByClassName('min_contener')[0].classList.add('padding0');
                            imgHeight();
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
            } else if ( $rootScope.window != 'small' || $rootScope.window != 'medium') {
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
