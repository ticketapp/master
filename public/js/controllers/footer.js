app.controller('footerCtrl', function ($scope, $modal) {
    $scope.openIssues =function () {
        var modalInstance = $modal.open({
            templateUrl: 'assets/partials/modalFooter.html',
            controller: 'modalFooterCtrl'
        });

        modalInstance.result.then();
    }
});
app.controller('modalFooterCtrl', function ($scope, $modalInstance) {
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});