app.controller ('CreateEventCtrl', function ($scope){
    
    $scope.tabs = [];
    $scope.addTab = function(){
        $scope.tabs.push($scope.newTab);
        $scope.newTab = {};
    };


});