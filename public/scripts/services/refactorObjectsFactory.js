angular.module('claudeApp').
    factory('RefactorObjectsFactory', function () {
        var factory = {
            refactorArtistObject: function (artist) {
                artist.artist.genres = artist.genres.map(function (genre) {
                    return genre.genre
                });
                artist = artist.artist;
                return artist
            },
            normalizeEventObject : function (event) {
                event.event.addresses = event.addresses;
                event.event.artists = event.artists.map(function(artist) {
                    return factory.refactorArtistObject(artist)
                });
                event.event.genres = event.genres;
                event.event.organizers = event.organizers.map(function(organizer){return organizer.organizer});
                event.event.places = event.places;
                return event.event;
            }
        };
        return factory;
    });