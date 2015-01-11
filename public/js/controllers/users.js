app.controller ('UsersCtrl', function ($scope, UserFactory, $routeParams, $modal){
	var user = UserFactory.getUser($routeParams.id).then(function(user){
        $scope.newTab = {};
        $scope.user = user;
	}, function(msg){
		alert (msg);
		});
    
   $scope.addTab = function(){
   $http.post("/teest", { time: (new Date()).toUTCString() });
        $scope.user.tabs.push($scope.newTab);


        $scope.newTab = {};
    };
});
