app.controller ('EventViewCtrl', function ($scope, $routeParams, $http ){
    $http.get('/events/' + $routeParams.id)
        .success(function(data, status){
            $scope.event = data;
            console.log(data.name);
        }).error(function(data, status){
            console.log(data);
        });
    angular.element.ready(function () {
        var eventInfoConteners = document.getElementsByClassName('eventInfo');
        for (var i = 0; i < eventInfoConteners.length; i++) {
            console.log(eventInfoConteners[i].offsetLeft)
            if (eventInfoConteners[i].offsetLeft < 30) {
                eventInfoConteners[i].classList.remove('large-4');
                eventInfoConteners[i].classList.add('large-12');
            }
        }
    });
});
