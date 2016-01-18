angular.module('claudeApp').
    directive('playlist', [
        function () {
            return {
                restrict : 'E',
                templateUrl: 'assets/components/playlist/playlistTemplate.html',
                scope : true
            }
        }]);