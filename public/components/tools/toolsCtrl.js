angular.module('claudeApp').controller('toolsCtrl', ['$scope', '$modal', '$log', '$rootScope',
    function ($scope, $modal, $log, $rootScope) {
    $scope.connected = $rootScope.connected;
    $scope.open = function () {
        var modalInstance = $modal.open({
            templateUrl: 'assets/components/tools/tools.html',
            controller: 'ModalInstanceCtrl',
            resolve: {
                items: function () {
                    return $scope.items;
                },
                connected: function () {
                    return $scope.connected;
                }
            }
        });

        modalInstance.result.then(function (selectedItem) {
            $scope.selected = selectedItem;
        }, function () {
            $log.info('Modal dismissed at: ' + new Date());
        });
    };
}]);

// Please note that $modalInstance represents a modal window (instance) dependency.
// It is not the same as the $modal service used above.

angular.module('claudeApp').controller('ModalInstanceCtrl', ['$scope', '$modalInstance', '$rootScope',
'$http', 'InfoModal', 'UserFactory',
    function ($scope, $modalInstance, $rootScope, $http, InfoModal, UserFactory) {
    $scope.logout = function () {
        $http.get('/logout').
            success(function (data) {
                $rootScope.connected = false;
                InfoModal.displayInfo('vous êtes deconnecté')
            })
    };
    $scope.getPlaylists = function() {
        $http.get('/playlists').
            success(function(data) {
                $scope.playlists = data;
                console.log(data)
            })
    };
    $scope.getFavoritesTracks = function() {
        UserFactory.getFavoritesTracks().then(function (tracks) {
            console.log(tracks)
            $scope.favorites = tracks;
        })
    };
    $scope.deletePlaylist = function (playlistId, index) {
        $http.delete('/playlists/' + playlistId).
            success(function (data) {
                $scope.playlists.splice(index, 1)
            }).
            error (function (data) {
        })
    };
    $scope.ok = function () {
        $modalInstance.close();
    };
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
}]);

