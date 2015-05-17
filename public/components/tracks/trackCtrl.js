angular.module('claudeApp').controller('TrackCtrl', ['$scope', 'UserFactory', '$localStorage', '$modal',
    'TracksRecommender',
    function ($scope, UserFactory, $localStorage, $modal, TracksRecommender) {
        $scope.addTrackToFavorite = function (trackId) {
            UserFactory.AddTrackToFavorite(trackId)
        };

        $scope.removeFromFavorites = function (trackId) {
            UserFactory.removeFromFavorites(trackId)
        };
        $scope.signalTrack = function (trackId, index) {
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
                controller: 'SignalTrackCtrl'
            });
            modalInstance.result.then(function () {
                $localStorage.tracksSignaled.push(trackId);
                TracksRecommender.UpsertTrackRate(false, trackId);
                $scope.closeTrack(index);
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