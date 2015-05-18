angular.module('claudeApp').controller('contactCtrl', ['$scope', '$http', 'StoreRequest', 'InfoModal',
    function ($scope, $http, StoreRequest, InfoModal) {
    $scope.message = {};
    $scope.success = '';
    $scope.postMsg = function (message) {
        $http.post('/mails', {subject: message.subject, message: message.content}).
            success(function () {
                $scope.success = 'votre message à bien été posté';
            }).
            error(function (data) {
                if (data.error == 'Credentials required') {
                    var object = {subject: message.subject, message: message.content};
                    StoreRequest.storeRequest('post', '/mails', object, 'votre message à bien été posté');
                } else {
                    InfoModal.displayInfo('Désolé une erreur s\'est produite');
                }
            })
    }
}]);