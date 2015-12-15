angular.module('claudeApp').factory('TrackService', ['$localStorage', '$filter', 'FollowService', '$q', '$http',
    function($localStorage, $filter, FollowService, $q, $http) {
    var factory = {
        /*
         class Track {
         constructor(uuid, title, url, platform, thumbnailUrl, artistFacebookUrl, artistName, redirectUrl, confidence,
         playlistRank, genres) {
         this.uuid = uuid;
         this.title = title;
         this.url = url;
         this.platform = platform;
         this.thumbnailUrl = thumbnailUrl;
         this.artistFacebookUrl = artistFacebookUrl;
         this.artistName = artistName;
         this.redirectUrl = redirectUrl;
         this.confidence = confidence;
         this.playlistRank = playlistRank;
         this.genres = genres;
         }
         }
         */

        filterSignaledTracks: function(tracks) {
            return tracks.filter(function (track) {
                if ($filter('filter')($localStorage.tracksSignaled, {trackId: track.uuid}).length === 0) {
                    return track;
                }
            })
        },

        getArtistTracks: function(facebookUrl) {
            var deferred = $q.defer();
            $http.get('/artists/' + facebookUrl + '/tracks?numberToReturn=0&offset=0').
                success(function(tracks) {
                    deferred.resolve(tracks);
                }).error(function (error) {
                    deferred.resolve([]);
                });

            return deferred.promise;
        },

        countRates: function(tracks) {
            var ratedTracks = tracks.filter(function(track) {
                if (track.confidence !== undefined && track.confidence > 5000) {
                    return track;
                }
            });
            return ratedTracks.length
        },

        setFavorite: function(tracks) {
            var defered = $q.defer();
            FollowService.tracks.favorites().then(function(favoritesTracks) {
                var tracksWithFavorites =  tracks.map(function(track) {
                    if ($filter('filter')(favoritesTracks, {uuid: track.uuid}).length > 0) {
                        track.isFavorite = true;
                    }
                    return track;
                });
                defered.resolve(tracksWithFavorites);
            }, function error () {
                defered.resolve(tracks)
            });
            return defered.promise;
        }
    };
    return factory;
}]);