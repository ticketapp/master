app.controller('scrollCtrl', ['$scope','$rootScope', '$location', '$timeout', '$anchorScroll',
    function ($scope, $rootScope, $location, $anchorScroll, $timeout) {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function(position)
                {
                    //alert("Latitude : " + position.coords.latitude + ", longitude : " + position.coords.longitude);
                }, function erreurPosition(error) {
                }
            );
        } else {
        }
      $scope.gotoTop = '';
        function location() {
            $timeout(function(){
                $location.hash('top');
                $anchorScroll();
            }, 200);
            if ($location.path() == '/') {
                $rootScope.home = true;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
            } else if ($location.path() == '/search'){
                $rootScope.pathArt = false;
                $rootScope.home = false;
                $rootScope.pathEvent = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = true;
            } else if ($location.path().indexOf('/artiste') > -1){
                $rootScope.pathArt = true;
                $rootScope.home = false;
                $rootScope.pathEvent = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
            } else if ($location.path().indexOf('/event') > -1){
                $rootScope.pathEvent = true;
                $rootScope.pathArt = false;
                $rootScope.home = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
            } else if ($location.path().indexOf('/user') > -1){
                $rootScope.pathUsr = true;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.home = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
            } else if ($location.path().indexOf('/lieu') > -1){
                $rootScope.pathUsr = false;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.home = false;
                $rootScope.pathPlace = true;
                $rootScope.pathSearch = false;
            } else {
                $rootScope.pathUsr = false;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.home = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
            }
        }
        $scope.$on('$locationChangeSuccess', function(){
            location()
        });
        $rootScope.activArtist = false;
        $rootScope.activEvent = true;
        $rootScope.activPlace = false;
        $rootScope.activUsr = false;

    }

]);

