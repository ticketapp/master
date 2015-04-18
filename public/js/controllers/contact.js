app.controller('contactCtrl', function ($scope, $http) {
    $scope.message = {}
    $scope.postMsg = function (message) {
        console.log(message)
    }
})