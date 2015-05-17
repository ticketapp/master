angular.module('claudeApp').directive('ngTrack', function () {
    return {
        restrict: 'E',
        templateUrl: 'assets/components/tracks/track.html',
        controller: 'TrackCtrl'
    }
});