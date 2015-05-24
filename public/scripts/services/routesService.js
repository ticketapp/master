angular.module('claudeApp').factory('RoutesFactory', function () {
    var factory = {
        places : {
            getPlacesPassedEvents: function (placeId) {
                return '/places/' + placeId + '/passedEvents';
            }
        },
        organizers : {
            getOrganizersPassedEvents: function (organizerId) {
                return '/organizers/' + organizerId + '/passedEvents';
            },
            getFollowedOrganizers: function () {
                return '/organizers/followed/';
            }
        }
    };
    return factory;
});