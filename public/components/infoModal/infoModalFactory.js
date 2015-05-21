angular.module('claudeApp').factory('InfoModal', ['$modal', '$mdToast',
    function ($modal, $mdToast) {
    var factory = {
        displayInfo : function (info, type) {
            if (type == undefined || type != 'error') {
                $mdToast.show(
                    $mdToast.simple()
                        .content(info)
                        .position('true, true, false, false')
                        .hideDelay(3000)
                );
            } else {
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
        }
    };
    return factory;
}]);