angular.module('claudeApp').factory('StoreRequest', ['$modal', '$rootScope',
    function ($modal, $rootScope) {
        var factory = {
            storeRequest : function (method, path, object, success, error) {
                var modalInstance = $modal.open({
                    templateUrl: 'assets/components/connect/connectionModal.html',
                    controller: 'ConnectionModalCtrl',
                    resolve: {
                        connected: function () {
                            return $rootScope.connected;
                        }
                    }
                });
                modalInstance.result.then(function (selectedItem) {
                }, function () {
                    $log.info('Modal dismissed at: ' + new Date());
                });
                $rootScope.lastReq = {
                    'method': method, 'path': path, 'object':object, 'success':success, 'error': error
                };

            }
        };
        return factory;
    }]);