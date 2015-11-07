angular.module('claudeApp').factory('TrackService', ['$localStorage', '$filter', 'UserFactory',
    function($localStorage, $filter, UserFactory) {
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
            if ($rootScope.connected === true) {
                UserFactory.getFavoritesTracks().then(function(favoritesTracks) {
                    var tracksWithFavorites =  tracks.map(function(track) {
                        if ($filter('filter')(favoritesTracks, {uuid: track.uuid}).length > 0) {
                            track.isFavorite = true;
                        }
                        return track;
                    });
                    defered.resolve(tracksWithFavorites);
                })
            } else { defered.resolve(tracks) }
            return defered.promise;
        }
    };
    return factory;
}]);