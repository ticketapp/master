app.controller('footerCtrl', function ($scope, $modal) {
    $scope.openIssues = function (content) {
        var modalInstance = $modal.open({
            templateUrl: 'assets/partials/modalFooter.html',
            controller: 'modalFooterCtrl',
            resolve: {
                content: function () {
                    return content;
                }
            }
        });

        modalInstance.result.then();
    }
});
app.controller('modalFooterCtrl', function ($scope, $modalInstance, content) {
    $scope.content = content;
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});