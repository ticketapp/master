app.controller ('EventsCtrl', function ($scope, EventFactory){
    $scope.events = EventFactory.getEvents().then(function(events){
        $scope.events = events;
        }, function(msg){
            alert(msg);
        })
});