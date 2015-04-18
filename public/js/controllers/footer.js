app.controller('footerCtrl', function ($scope, $modal) {
    $scope.openIssues =function () {
        var modalInstance = $modal.open({
            templateUrl: 'assets/partials/_issues.html',
            controller: 'modalFooterCtrl'
        });

        modalInstance.result.then();
    }
});
app.controller('modalFooterCtrl', function ($scope, $modalInstance) {

});