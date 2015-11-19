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
'$http', 'InfoModal', 'UserFactory', 'PlaylistService', 'ArtistsFactory', 'PlaceFactory', 'OrganizerFactory', 'FollowService',
    'RefactorObjectsFactory',
    function ($scope, $modalInstance, $rootScope, $http, InfoModal, UserFactory, PlaylistService, ArtistsFactory, PlaceFactory,
              OrganizerFactory, FollowService, RefactorObjectsFactory) {
    $scope.suggeredPlaylists = [];
    $scope.playlists = [];
    $scope.logout = function () {
        $http.get('/signOut').
            success(function (data) {
                $rootScope.connected = false;
                InfoModal.displayInfo('vous êtes deconnecté')
            })
    };
    $scope.getPlaylists = function() {
        $http.get('/playlists').
            success(function(playlists) {
                playlists = playlists.map(function(playlist) {
                    return RefactorObjectsFactory.refactorPlaylistObject(playlist)
                });
                $scope.playlists = playlists;
            })
    };
    $scope.getPlaylists();
    $scope.viewPlaylists = true;

    function updatePlaylist(update) {
        var playlistToUpdate = $scope.suggeredPlaylists.filter(function (playlist) {
            return playlist.uuid === update.uuid
        });
        if (playlistToUpdate.length > 0) {
            $scope.suggeredPlaylists[$scope.suggeredPlaylists.indexOf(playlistToUpdate[0])] = update
        } else {
            $scope.suggeredPlaylists.push(update)
        }
    }

    PlaylistService.getEventsPlaylist().then(function (completePlaylist) {
        updatePlaylist(completePlaylist);
    }, function(error) {

    }, function(update) {
        updatePlaylist(update);
    });

    PlaylistService.getEventsGenrePlaylist('electro').then(function (completePlaylist) {
        updatePlaylist(completePlaylist);
    }, function(error) {

    }, function(update) {
        updatePlaylist(update);
    });

    PlaylistService.getEventsGenrePlaylist('reggae').then(function (completePlaylist) {
        updatePlaylist(completePlaylist);
    }, function(error) {

    }, function(update) {
        updatePlaylist(update);
    });

    PlaylistService.getEventsGenrePlaylist('rock').then(function (completePlaylist) {
        updatePlaylist(completePlaylist);
    }, function(error) {

    }, function(update) {
        updatePlaylist(update);
    });

    PlaylistService.getEventsGenrePlaylist('jazz').then(function (completePlaylist) {
        updatePlaylist(completePlaylist);
    }, function(error) {

    }, function(update) {
        updatePlaylist(update);
    });

    PlaylistService.getEventsGenrePlaylist('hip-hop').then(function (completePlaylist) {
        updatePlaylist(completePlaylist);
    }, function(error) {

    }, function(update) {
        updatePlaylist(update);
    });

    PlaylistService.getEventsGenrePlaylist('chanson').then(function (completePlaylist) {
        updatePlaylist(completePlaylist);
    }, function(error) {

    }, function(update) {
        updatePlaylist(update);
    });

    $scope.favorites = {};
    $scope.getFavorites = function() {
        $scope.favorites.name = 'favories';
        FollowService.tracks.favorites().then(function (tracks) {
            $scope.favorites.tracks = tracks;
            $scope.closeTrack = function (index, trackId) {
                FollowService.tracks.removeFromFavorites(trackId);
                $scope.favorites.tracks.tracks.splice(index, 1);
            };
        });
        FollowService.artists.followed().then(function (artists) {
            $scope.favorites.artists = artists;
        });
        FollowService.places.followed().then(function (places) {
            $scope.favorites.places = places;
        });
        FollowService.organizers.followed().then(function (organizers) {
            $scope.favorites.organizers = organizers;
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

    // playlist generator //
    /*$scope.SongAttributesSearch = [
        {name: 'description', value: '', type:'input'},
        {name: 'style', value: '', type:'input'},
        {name: 'mood', value: '', type:'input'},
        {name: 'song_type', value: '', type:'select', options:
            ['christmas', 'live', 'studio', 'acoustic', 'electric']
        },
        {name: 'max_tempo', value: '', type: 'scale', min: 0.0, max:500.0},
        {name: 'min_tempo', value: '', type: 'scale', min: 0.0, max:500.0},
        {name: 'max_duration', value: '', type: 'scale', min: 0.0, max:3600.0},
        {name: 'min_duration', value: '', type: 'scale', min: 0.0, max:3600.0},
        {name: 'max_loudness', value: '', type: 'scale', min: -100.0, max:100.0},
        {name: 'min_loudness', value: '', type: 'scale', min: -100.0, max:100.0},
        {name: 'artist_max_familiarity', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'artist_min_familiarity', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'artist_start_year_before', value: '', type: 'year'},
        {name: 'artist_start_year_after', value: '', type: 'year'},
        {name: 'artist_end_year_before', value: '', type: 'year'},
        {name: 'artist_end_year_after', value: '', type: 'year'},
        {name: 'song_max_hotttnesss', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'song_min_hotttnesss', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'artist_max_hotttnesss', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'artist_min_hotttnesss', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'min_longitude', value: '', type: 'scale', min: -180.0, max:180.0},
        {name: 'max_longitude', value: '', type: 'scale', min: -180.0, max:180.0},
        {name: 'min_latitude', value: '', type: 'scale', min: -90.0, max:90.0},
        {name: 'max_latitude', value: '', type: 'scale', min: -90.0, max:90.0},
        {name: 'max_danceability', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'min_danceability', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'max_energy', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'min_energy', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'max_liveness', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'min_liveness', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'max_speechiness', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'min_speechiness', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'max_acousticness', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'min_acousticness', value: '', type: 'scale', min: 0.0, max:1.0},
        {name: 'mode', value: '', type: 'select', options: [0, 1]}
    ];

    $scope.searchTracksByFilters = function () {
        var listAttributes = '';
        for (var i = 0; i < $scope.SongAttributesSearch.length; i++) {
            console.log($scope.SongAttributesSearch[i].value)
            if ($scope.SongAttributesSearch[i].value != '' && $scope.SongAttributesSearch[i].value != 0) {
                listAttributes = listAttributes + '&' + $scope.SongAttributesSearch[i].name +
                    '=' + $scope.SongAttributesSearch[i].value;
            }
        }
        console.log(listAttributes)
        $http.get('http://developer.echonest.com/api/v4/song/search?' +
            'api_key=3ZYZKU3H3MKR2M59Z&format=json&results=100' + listAttributes + '&bucket=id:facebook').
            success(function (data) {
                console.log(data);
            })
    }
    template :
     <form>
     <div ng-repeat="attr in SongAttributesSearch" class="column large-6">
     {{attr.name}}
     <input type="text" ng-model="attr.value" ng-show="attr.type == 'input'">
     <md-slider flex
     ng-model="attr.value" min="{{attr.min}}" max="{{attr.max}}" step="0.1"
     aria-label="{{attr.name}}"
     ng-show="attr.type == 'scale'"
     id="attr.name"
     class="md-primary column small-12 padding0 margin0">
     </md-slider>
     </div>
     <button ng-click="searchTracksByFilters()">Generate</button>
     </form>
    */
}]);

