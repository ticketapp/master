angular.module('claudeApp').controller('savePlaylistCtrl', ['$scope', '$rootScope', '$modalInstance',
    '$http', '$modal', 'UserFactory', 'InfoModal', 'StoreRequest', 'TracksRecommender', '$timeout',
    function ($scope, $rootScope, $modalInstance, $http, $modal, UserFactory, InfoModal, StoreRequest,
              TracksRecommender, $timeout) {
    if ($rootScope.playlist.name.length > 0) {
        $scope.newPlaylist = false;
    }
    $scope.createNewPlaylist = function (playlist) {
        $modalInstance.dismiss('cancel');
        var tracksToSave = [];
        $scope.newPlaylist = true;
        $timeout(function () {
            console.log(playlist.tracks)
            for (var i=0; i < playlist.tracks.length; i++) {
                TracksRecommender.UpsertTrackRate(true, playlist.tracks[i].uuid);
                tracksToSave.push({trackId: playlist.tracks[i].uuid, trackRank: i})
            }
            $http.post('/playlists', {name: playlist.name, trackIds: tracksToSave}).
                success(function (data) {
                    InfoModal.displayInfo('votre playlist ' + playlist.name + ' est enregistrée');
                }).error(function (data, status) {
                    if (status === 401) {
                        var object = {name: playlist.name, tracksId: tracksToSave};
                        StoreRequest.storeRequest('post', '/playlists', object, 'votre playlist "' + playlist.name + '" est enregistée')
                    } else {
                        InfoModal.displayInfo('Désolé une erreur s\'est produite', 'error');
                    }
                })
        }, 500)
    };

        //change to update
    $scope.updatePlaylist = function (playlist) {
        UserFactory.deletePlaylist(playlist.playlistId).then(function (del) {
            $scope.createNewPlaylist(playlist)
        })
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
}]);