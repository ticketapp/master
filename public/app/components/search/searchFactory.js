angular.module('claudeApp').factory('SearchFactory', ['$rootScope', function ($rootScope) {
    var factory = {
        initSearch : false,
        init : function () {
            if (factory.initSearch == false) {
                $rootScope.activArtist = false;
                $rootScope.activEvent = true;
                $rootScope.activPlace = false;
                $rootScope.activUsr = false;
                $rootScope.maxStart = 30;
                $rootScope.maxStartView = 168;
                $rootScope.redirectToSearch = function (research) {
                    $rootScope.storeSearch = research;
                    if ($location.path() != '/') {
                        $location.path('/search')
                    }
                };
                $rootScope.remStoreSearch = function () {
                    $rootScope.storeSearch = '';
                };
                factory.initSearch = true;
            }
        }
    };
    factory.init();
    return factory;
}]);