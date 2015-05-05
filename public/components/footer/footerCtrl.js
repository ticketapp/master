angular.module('claudeApp').controller('footerCtrl', function ($scope, $modal) {
    $scope.openIssues = function (content) {
        var modalInstance = $modal.open({
            templateUrl: 'assets/components/footer/modalFooter.html',
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
angular.module('claudeApp').controller('modalFooterCtrl', function ($scope, $modalInstance, content) {
    $scope.content = content;
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});