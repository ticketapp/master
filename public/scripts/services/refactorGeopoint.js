angular.module('claudeApp').
    factory('RefactorGeopoint', function () {
        var factory = {
            geoPoint : false,
            refactorGeopoint : function (geoPoint) {
                factory.geoPoint = geoPoint.replace("(", "").replace(")", "").replace("POINT ", "").replace(" ", ", ");
                return factory.geoPoint
            }
        };
        return factory;
    });