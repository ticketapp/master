app.module('claudeApp').factory('ToolsFactory', ['$http', '$q', '$rootscope', 'EventsFactory',
    function ($http, $q, $rootscope, EventsFactory) {
    var factory = {
        eventsPlaylist : {
            geopoint: '',
            playlist: []
        },
        getArtistTracks : function (track) {
            factory.eventsPlaylist.playlist.push(artist)
        },
        getEventsPlaylist : function () {
            var deferred = $q.defer();
            if ($rootscope.geoLoc == factory.eventsPlaylist.geopoint) {
                deferred.resolve(factory.eventsPlaylist.playlist)
            } else {
                EventsFactory.getEvents(336, $rootscope.geoLoc, 0).then(function(events) {
                    //artists forEach -- getTopTracks(numberToReturn) and addIt to playlist
                })
            }
        }
    }
}]);