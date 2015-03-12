app.controller ('EventViewCtrl',['$scope', '$routeParams', '$http', '$rootScope',
    function ($scope, $routeParams, $http, $rootScope ){
    $scope.map = false;
    $http.get('/events/' + $routeParams.id)
        .success(function(data, status){
            /*data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace("(", "");
            data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace(")", "");
            data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace(",", ", ");*/
            $scope.event = data;
            console.log($scope.event);
            if ( $rootScope.window != 'small' || $rootScope.window != 'medium') {
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
                        $scope.needConnect = true;
                    }
                })
        };
}]);
