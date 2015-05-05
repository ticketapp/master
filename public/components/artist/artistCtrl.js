angular.module('claudeApp').
controller('ArtistCtrl', ['$scope', '$localStorage', 'ArtistsFactory', '$timeout', '$filter',
        '$modal', '$rootScope', '$routeParams', 'WebsitesFactory',
    function ($scope, $localStorage, ArtistsFactory, $timeout, $filter, $modal, $rootScope,
              $routeParams, WebsitesFactory) {

        $scope.trackLimit = 12;
        $scope.trackTitle = '';
        $scope.showDesc = false;
        $scope.selectedTab = 0;
        if ($rootScope.artisteToCreate == false) {
            $scope.tracks = [];
            $scope.artist = [];
            $scope.artist.events = [];
            $rootScope.loadingTracks = true;
            ArtistsFactory.getArtist($routeParams.facebookUrl).then(function (artist) {
                $scope.artist = artist;
                $scope.tracks = artist.tracks;
                $rootScope.loadingTracks = false;
                if (artist.websites != undefined) {
                    $scope.websites = WebsitesFactory.normalizeWebsitesObject(artist.websites,
                        $routeParams.facebookUrl);
                }
                ArtistsFactory.getArtistEvents(artist.artistId).then(function (events) {
                    $scope.artist.events = events;
                })
            });
        } else {
            $scope.selectedTab = 1;
            ArtistsFactory.passArtisteToCreateToFalse();
        }

        if ($localStorage.tracksSignaled == undefined) {
            $localStorage.tracksSignaled = [];
        }

        $scope.follow = function () {
            ArtistsFactory.followArtistByArtistId($scope.artist.artistId).then(function (followed) {
                console.log(followed)
            })
        };

        $scope.filterTracks = function () {
            $timeout(function () {
                $scope.$apply(function(){
                    $scope.artist.tracks = $filter('filter')($scope.tracks,
                        {title: $scope.trackTitle})
                })
            }, 0)
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
                    '<option value="Q">Mauvaise qualité</option>'+
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
                        $scope.tracks.splice(i, 1);
                        tracksLength --;
                    }
                }
            }, function () {
            });

        }
}]);

angular.module('claudeApp').controller('SignalTrackCtrl', function ($scope, $modalInstance) {

    $scope.ok = function (reason) {
        if (reason != undefined) {
            console.log(reason);
            $modalInstance.close();
        } else {
            $scope.error = 'veuyez renseigner ce champs'
        }
    };
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});