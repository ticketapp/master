angular.module('claudeApp').
    factory('RefactorObjectsFactory', ['RefactorGeopoint', function (RefactorGeopoint) {
        var factory = {
            refactorArtistObject: function(artist) {
                if (artist.genres) {
                    artist.artist.genres = artist.genres.map(function (genre) {
                        return genre.genre
                    });
                } else {
                    artist.artist.genres = []
                }
                artist = artist.artist;
                return artist
            },
            refactorTrackWithGenreObject: function(track) {
                track.track.genres = track.genres;
                track = track.track;
                return track
            },
            refactorOrganizerObject: function(organizer) {
                organizer = organizer.organizer;
                return organizer
            },
            refactorPlaceObject: function(place) {
                if(place.address) {
                    if(place.address.geographicPoint) {
                        place.address.geographicPoint = RefactorGeopoint.refactorGeopoint(place.address.geographicPoint);
                    }
                    place.place.address = place.address;
                }
                place = place.place;
                switch(place.geographicPoint) {
                    case undefined:
                        break;
                    default :
                        place.geographicPoint = RefactorGeopoint.refactorGeopoint(place.geographicPoint);
                        break
                }
                return place
            },
            normalizeEventObject : function(event) {
                event.event.addresses = event.addresses.map(function(address) {
                    if (address.geographicPoint) {
                        address.geographicPoint = RefactorGeopoint.refactorGeopoint(address.geographicPoint);
                    }
                    return address;
                });
                if (event.event.geographicPoint) {
                    event.event.geographicPoint = RefactorGeopoint.refactorGeopoint(event.event.geographicPoint);
                } else if (event.addresses[0]) {
                    if(event.addresses[0].geographicPoint) {
                        event.event.geographicPoint = event.addresses[0].geographicPoint
                    }
                }
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
            },
            refactorPlaylistObject: function(playlist) {
                var refactoredPlaylist = playlist.playlistInfo;
                refactoredPlaylist.tracks = playlist.tracksWithRankAndGenres.map(function(track) {
                    return factory.refactorTrackWithGenreObject(track.track);
                });
                return refactoredPlaylist;
            }
        };
        return factory;
    }]);