app.controller('contactCtrl', function ($scope, $http, $modal, $rootScope) {
    $scope.message = {}
    $scope.success = '';
    $scope.postMsg = function (message) {
        $http.post('/mails', {subject: message.subject, message: message.content}).
            success(function () {
                $scope.success = 'votre message à bien été posté';
            }).
            error(function (data) {
                if (data.error == 'Credentials required') {
                    var object = {subject: message.subject, message: message.content};
                    $rootScope.storeLastReq('post', '/mails', object, 'votre message à bien été posté');
                } else {
                    $scope.info = 'Désolé une erreur s\'est produite';
                    var modalInstance = $modal.open({
                        templateUrl: 'assets/partials/_infoModal.html',
                        controller: 'infoModalCtrl',
                        resolve: {
                            info: function () {
                                return $scope.info;
                            }
                        }
                    });
                    modalInstance.result.then(function () {
                        $log.info('Modal dismissed at: ' + new Date());
                    });
                }
            })
    }
})