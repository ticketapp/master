'use strict';
angular.module('claudeApp')
    .controller('headerCtrl', ['$scope', 'DetectSize', 'DetectPath', 'GeolocFactory', '$rootScope', 'UserFactory',
        function ($scope, DetectSize, DetectPath, GeolocFactory, $rootScope, UserFactory) {
            UserFactory.makeFavoriteTracksRootScope();
    }]);