angular.module('claudeApp').
    factory('RefactorObjectsFactory', function () {
        var factory = {
            refactorArtistObject: function (artist) {
                artist.artist.genres = artist.genres.map(function(genre) {
                    return genre.genre
                });
                artist = artist.artist;
                return artist
            },
            refactorTrackWithGenreObject: function(track) {
                track.track.genres = track.genres.map(function(genre) {
                    return genre
                });
                track = track.track;
                return track
            },
            refactorOrganizerObject: function(organizer) {
                organizer = organizer.organizer;
                return organizer
            },
            refactorPlaceObject: function(place) {
                place = place.place;
                return place
            },
            normalizeEventObject : function (event) {
                event.event.addresses = event.addresses;
                event.event.artists = event.artists.map(function(artist) {
                    return factory.refactorArtistObject(artist)
                });
                event.event.genres = event.genres;
                event.event.organizers = event.organizers.map(function(organizer) {
                    return factory.refactorOrganizerObject(organizer)
                });
                event.event.places = event.places.map(function(place) {
                    return factory.refactorPlaceObject(place)
                });
                return event.event;
            }
        };
        return factory;
    });