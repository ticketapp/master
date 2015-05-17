angular.module('claudeApp').directive('ngTrack', function () {
    return {
        restrict: 'E',
        templateUrl: 'assets/components/tracks/track.html',
        controller: 'TrackCtrl'
    }
});

angular.module('claudeApp').directive('ngPlaylistTrack', function () {
    return {
        restrict: 'E',
        templateUrl: 'assets/components/tracks/playlistTrack.html',
        controller: 'TrackCtrl'
    }
});