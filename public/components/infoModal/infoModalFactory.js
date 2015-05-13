angular.module('claudeApp').factory('InfoModal', ['$modal', function ($modal) {
    var factory = {
        displayInfo : function (info) {
            var modalInstance = $modal.open({
                templateUrl: 'assets/components/infoModal/infoModal.html',
                controller: 'infoModalCtrl',
                resolve: {
                    info: function () {
                        return info;
                    }
                }
            });
            modalInstance.result.then(function () {
            });
        }
    };
    return factory;
}]);