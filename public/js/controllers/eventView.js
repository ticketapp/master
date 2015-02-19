app.controller ('EventViewCtrl', function ($scope, $routeParams, $http ){
    $scope.map = false;
    $http.get('/events/' + $routeParams.id)
        .success(function(data, status){
            data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace("(", "");
            data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace(")", "");
            data.addresses[0].geographicPoint = data.addresses[0].geographicPoint.replace(",", ", ");
            $scope.event = data;
            $scope.map = true;
            console.log($scope.event);
            if (data.description.length > 0) {
                var waitForBinding = setInterval(function () {
                    if (document.getElementById('eventDescBind').innerHTML.length > 0) {
                        clearInterval(waitForBinding);
                        var eventInfoConteners = document.getElementsByClassName('eventInfo');
                        for (var i = 0; i < eventInfoConteners.length; i++) {
                            console.log(eventInfoConteners[i].offsetLeft);
                            if (eventInfoConteners[i].offsetLeft < 30) {
                                eventInfoConteners[i].classList.remove('large-4');
                                eventInfoConteners[i].classList.add('large-12');
                            }
                        }
                    }
                }, 100);
            }
        }).error(function(data, status){
            console.log(data);
        });
});
