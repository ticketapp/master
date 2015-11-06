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
                return '/organizers/followed';
            }
        },
        user : {
            getRemovedTracks : function () {
                return '/users/tracksRemoved'
            }
        }
    };
    return factory;
});