app.controller ('EventViewCtrl',['$scope', '$routeParams', '$http', '$rootScope',
    function ($scope, $routeParams, $http, $rootScope ){
    $scope.map = false;
    $http.get('/events/' + $routeParams.id)
        .success(function(data, status){
            data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace("(", "");
            data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace(")", "");
            data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace(",", ", ");
            $scope.event = data;
            $scope.map = true;
            console.log($scope.event);
            if (data.description.length > 0 && $rootScope.window != 'small' && $rootScope.window != 'medium') {
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
                    }
                }, 100);
            } else if ( $rootScope.window != 'small' || $rootScope.window != 'medium') {
                $rootScope.$watch('window', function(newval) {
                    if (newval == 'large' || newval == 'xlarge' || newval == 'xxlarge') {
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
                            }
                        }, 100);
                    }
                })
            }
        }).error(function(data, status){
            console.log(data);
        });
}]);
