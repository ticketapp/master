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
                        for (var i = 0; i < eventInfoConteners.length; i++) {
                            if (eventInfoConteners[i].offsetLeft < 30) {
                                eventInfoConteners[i].classList.remove('large-4');
                                eventInfoConteners[i].classList.add('large-12');
                            }
                        }
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
