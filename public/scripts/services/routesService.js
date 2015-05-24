angular.module('claudeApp').factory('RoutesFactory', function () {
    var factory = {
        getPlacesPassedEvents : function (id) {
            return '/places/'+ id + '/passedEvents';
        }
    };
    return factory;
});