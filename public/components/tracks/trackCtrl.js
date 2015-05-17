angular.module('claudeApp').controller('TrackCtrl', ['$scope', 'UserFactory', '$localStorage', '$modal',
    'TracksRecommender',
    function ($scope, UserFactory, $localStorage, $modal, TracksRecommender) {
        $scope.addTrackToFavorite = function (trackId) {
            UserFactory.AddTrackToFavorite(trackId)
        };
        $scope.signalTrack = function (index) {
            var modalInstance = $modal.open({
                template: '<form ng-submit="ok(reason)">' +
                    '<b class="column large-12 center">Pour quelle raison souhaitez-vous signaler ' +
                    'ce morceau ? <br/>'+
                    'Attention, en signalant ce morceau il sera supprimé des morceaux que Claude ' +
                    'vous proppose</b>' +
                    '<select ng-model="reason">'+
                    '<option value="B">Mauvais Artist</option>'+
                    '<option value="Q">Mauvaise qualitée</option>'+
                    '</select><b class="column large-12">{{error}}</b>'+
                    '<input type="submit" class="button">'+
                    '<a class="button float-right" ng-click="cancel()">Annuler</a>'+
                    '</form>',
                controller: 'SignalTrackCtrl',
                resolve: {
                    index: function () {
                        return index;
                    }
                }
            });
            modalInstance.result.then(function () {
                var tracksLength = $scope.tracks.length;
                for (var i = 0; i < tracksLength; i++) {
                    if ($scope.tracks[i].trackId == $scope.artist.tracks[index].trackId) {
                        $localStorage.tracksSignaled.push($scope.tracks[i].trackId);
                        TracksRecommender.UpsertTrackRate(false, $scope.tracks[i].trackId);
                        $scope.tracks.splice(i, 1);
                        tracksLength --;
                    }
                }
                $scope.artist.tracks.splice(index, 1);
            }, function () {
            });

        }
    }]);


angular.module('claudeApp').controller('SignalTrackCtrl', ['$scope', '$modalInstance',
    function ($scope, $modalInstance) {
        $scope.ok = function (reason) {
            if (reason != undefined) {
                $modalInstance.close();
            } else {
                $scope.error = 'veuyez renseigner ce champs'
            }
        };
        $scope.cancel = function () {
            $modalInstance.dismiss('cancel');
        };
    }]);