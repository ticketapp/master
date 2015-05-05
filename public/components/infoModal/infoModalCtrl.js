angular.module('claudeApp').
    controller('infoModalCtrl', ['$scope', '$modalInstance', 'info',
         function ($scope, $modalInstance, info) {
    $scope.info = info;
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
    $scope.ok = function () {
        $modalInstance.close();
    };
}]);