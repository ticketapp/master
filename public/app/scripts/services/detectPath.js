angular.module('claudeApp')
    .factory('DetectPath', ['$location', '$rootScope', function ($location, $rootScope) {
        var path = $location.path();
        function getPath () {
            if (path === '/') {
                path = 'home';
            } else if (path === '/search') {
                path = 'search';
            } else if (path.indexOf('/artiste') > -1) {
                path = 'artist';
            } else if (path.indexOf('/event') > -1) {
                path = 'event';
            } else if (path.indexOf('/organizer') > -1) {
                path = 'organizer';
            } else if (path.indexOf('/lieu') > -1) {
                path = 'place';
            } else if (path.indexOf('/iframeEvents') > -1) {
                path = 'iframe';
            } else {
                path = false;
            }
            $rootScope.path = path;
        }
        getPath();
        $rootScope.$on('$locationChangeSuccess', function () {
            getPath()
        });
        return path;
    }]);