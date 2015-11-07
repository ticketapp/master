angular.module('claudeApp').controller('TrackCtrl', ['$scope', 'UserFactory', '$localStorage', '$modal',
    'TracksRecommender', '$rootScope',
    function ($scope, UserFactory, $localStorage, $modal, TracksRecommender, $rootScope) {
        $scope.addTrackToFavorite = function (track) {
            UserFactory.AddTrackToFavorite(track);
            $rootScope.favoritesTracks.push(track)
        };

        $scope.removeFromFavorites = function (trackUUID) {
            UserFactory.removeFromFavorites(trackUUID);
            $rootScope.favoritesTracks.splice($rootScope.favoritesTracks.indexOf(trackUUID), 1)
        };
        $scope.signalTrack = function (trackId, index) {
            var modalInstance = $modal.open({
                template: '<form ng-submit="ok(reason)">' +
                    '<b class="column large-12 center">Pour quelle raison souhaitez-vous signaler ' +
                    'ce morceau ? <br/>'+
                    'Attention, en signalant ce morceau il sera supprimé des morceaux que Claude ' +
                    'vous proppose</b>' +
                    '<select ng-model="reason">'+
                    '<option value="B">Mauvais artiste</option>'+
                    '<option value="Q">Mauvaise qualitée</option>'+
                    '</select><b class="column large-12">{{error}}</b>'+
                    '<input type="submit" class="button">'+
                    '<a class="button float-right" ng-click="cancel()">Annuler</a>'+
                    '</form>',
                controller: 'SignalTrackCtrl'
            });
            modalInstance.result.then(function (reason) {
                if (trackId != null && trackId != undefined) {
                    $localStorage.tracksSignaled.push({trackId: trackId, reason: reason});
                    TracksRecommender.UpsertTrackRate(false, trackId, reason);
                }
                $scope.closeTrack(index, trackId);
            }, function () {
            });

        }
    }]);


angular.module('claudeApp').controller('SignalTrackCtrl', ['$scope', '$modalInstance',
    function ($scope, $modalInstance) {
        $scope.ok = function (reason) {
            if (reason != undefined) {
                $modalInstance.close(reason);
            } else {
                $scope.error = 'veuyez renseigner ce champs'
            }
        };
        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    }]);