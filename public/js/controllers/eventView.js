	
	app.controller ('EventViewCtrl', function ($scope, EventFactory, $routeParams ){
	var event = EventFactory.getEvent($routeParams.id).then(function(event){
    $scope.newComment = {};
	$scope.name = event.nomEvent;
	$scope.artistes = event.artistes;
    $scope.event = event;
    $scope.comments = event.comments
	}, function(msg){
	alert (msg) ;
	});
    $scope.addComment = function(){
        $scope.event.comments.push($scope.newComment);       
        $scope.newComment = {};
    }
	});
