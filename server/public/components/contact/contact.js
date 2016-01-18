angular.module('claudeApp').controller('contactCtrl', ['$scope', '$http', 'StoreRequest', 'InfoModal',
    function ($scope, $http, StoreRequest, InfoModal) {
    $scope.message = {};
    $scope.success = '';
    $scope.postMsg = function (message) {
        $http.post('/mails', {subject: message.subject, message: message.content}).
            success(function () {
                InfoModal.displayInfo('Votre message a bien été posté');
            }).error(function (data, status) {
                if (status === 401) {
                    var object = {subject: message.subject, message: message.content};
                    StoreRequest.storeRequest('post', '/mails', object, 'Votre message a bien été posté');
                } else {
                    $scope.success = 'Désolé une erreur s\'est produite';
                }
            })
    }
}]);