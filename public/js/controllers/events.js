app.controller ('EventsCtrl', function ($scope, EventFactory){
    $scope.events = EventFactory.getEvents().then(function(events){
        $scope.events = events;
        }, function(msg){
            alert(msg);
        })
    /*var evnetsBlock = document.getElementsByClassName('data-ng-event');
    if (window.innerWidth >= 64.063em)*/
});