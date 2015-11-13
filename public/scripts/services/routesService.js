angular.module('claudeApp').factory('RoutesFactory', function () {
    var factory = {
        places : {
            getPlacesPassedEvents: function (placeId) {
                return '/places/' + placeId + '/passedEvents';
            },
            getFollowedPlaces: function () {
                return '/places/followed/';
            }
        },
        organizers : {
            getOrganizersPassedEvents: function (organizerId) {
                return '/organizers/' + organizerId + '/passedEvents';
            },
            getFollowedOrganizers: function () {
                return '/organizers/followed/';
            }
        },
        user : {
            getRemovedTracks : function () {
                return '/users/tracksRemoved'
            }
        },
        genres: {
          isAGenre: function(genre) {
              return '/genres/' + genre;
          }
        },
        city: {
          isACity: function(city) {
              return '/city/' + city;
          }
        },
        follow : {
            organizers : {
                followById: function (organizerId) {
                    return '/organizers/' + organizerId + '/followByOrganizerId';
                },
                unfollowById: function (organizerId) {
                    return '/organizers/' + organizerId + '/unfollowOrganizerByOrganizerId';
                },
                followByFacebookId: function (facebookId) {
                    return '/organizers/' + facebookId + '/followByFacebookId';
                },
                isFollowed: function (organizerId) {
                    return '/organizers/' + organizerId + '/isFollowed';
                },
                followed: function() {
                    return '/organizers/followed/';
                }
            },
            places : {
                followById: function (placeId) {
                    return '/places/' + placeId + '/followByPlaceId';
                },
                unfollowById: function (placeId) {
                    return '/places/' + placeId + '/unfollowPlaceByPlaceId';
                },
                followByFacebookId: function (facebookId) {
                    return '/places/' + facebookId + '/followByFacebookId';
                },
                isFollowed: function (placeId) {
                    return '/places/' + placeId + '/isFollowed';
                },
                followed: function() {
                    return '/places/followed/';
                }
            },
            artists : {
                followById: function (artistId) {
                    return '/artists/' + artistId + '/followByArtistId';
                },
                unfollowById: function (artistId) {
                    return '/artists/' + artistId + '/unfollowArtistByArtistId';
                },
                followByFacebookId: function (facebookId) {
                    return '/artists/' + facebookId + '/followByFacebookId';
                },
                isFollowed: function (artistId) {
                    return '/artists/' + artistId + '/isFollowed';
                },
                followed: function() {
                    return '/artists/followed/';
                }
            },
            tracks : {
                addToFavorites: function (trackId) {
                    return '/tracks/' + trackId + '/addToFavorites';
                },
                removeFromFavorites: function (trackId) {
                    return '/tracks/' + trackId + '/removeFromFavorites';
                },
                isFollowed: function (trackId) {
                    return '/tracks/' + trackId + '/isFollowed';
                },
                favorites: function() {
                    return '/tracks/favorites';
                }
            },
            events : {
                follow : function(eventId) {
                    return '/events/' + eventId + '/follow';
                },
                unfollow : function(eventId) {
                    return '/events/' + eventId +'/unfollow';
                },
                followed : function() {
                    return '/events/followed/';
                },
                isFollowed : function(eventId) {
                    return '/events/' + eventId + '/isFollowed';
                }
            }
        }
    };
    return factory;
});