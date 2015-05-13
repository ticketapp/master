angular.module('claudeApp').factory('SearchFactory', ['$rootScope', '$location',
    function ($rootScope, $location) {
    var factory = {
        initSearch : false,
        init : function () {
            console.log(factory.initSearch);
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
        },
        storeSearch : function (search) {
            $rootScope.storeSearch = search;
        }
    };
    factory.init();
    return factory;
}]);