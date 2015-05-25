angular.module('claudeApp').controller('toolsCtrl', ['$scope', '$modal', '$log', '$rootScope',
    function ($scope, $modal, $log, $rootScope) {
    $scope.connected = $rootScope.connected;
    $scope.open = function () {
        var modalInstance = $modal.open({
            templateUrl: 'assets/components/tools/tools.html',
            controller: 'ModalInstanceCtrl',
            windowClass: 'padding0',
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
'$http', 'InfoModal', 'UserFactory', 'ToolsFactory',
    function ($scope, $modalInstance, $rootScope, $http, InfoModal, UserFactory, ToolsFactory) {
    $scope.suggeredPlaylists = [];
    $scope.playlists = [];
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
    $scope.getPlaylists();
    $scope.viewPlaylists = true;

    ToolsFactory.getEventsPlaylist().then(function (playlist) {
        $scope.suggeredPlaylists.push(playlist)
    });

    ToolsFactory.getEventsGenrePlaylist('electro').then(function (playlist) {
        $scope.suggeredPlaylists.push(playlist)
    });

    ToolsFactory.getEventsGenrePlaylist('reggae').then(function (playlist) {
        $scope.suggeredPlaylists.push(playlist)
    });

    ToolsFactory.getEventsGenrePlaylist('rock').then(function (playlist) {
        $scope.suggeredPlaylists.push(playlist)
    });

    ToolsFactory.getEventsGenrePlaylist('jazz').then(function (playlist) {
        $scope.suggeredPlaylists.push(playlist)
    });

    ToolsFactory.getEventsGenrePlaylist('hip-hop').then(function (playlist) {
        $scope.suggeredPlaylists.push(playlist)
    });

    ToolsFactory.getEventsGenrePlaylist('chanson').then(function (playlist) {
        $scope.suggeredPlaylists.push(playlist)
    });
    $scope.getFavoritesTracks = function() {
        UserFactory.getFavoritesTracks().then(function (tracks) {
            $scope.favorites = {};
            $scope.favorites.name = 'favories';
            $scope.favorites.tracks = tracks;
            $scope.closeTrack = function (index) {
                $scope.favorites.splice(index, 1);
            };
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

