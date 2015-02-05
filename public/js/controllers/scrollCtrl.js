app.controller('scrollCtrl', ['$scope','$rootScope', '$location', '$timeout', '$anchorScroll',
    function ($scope, $rootScope, $location, $anchorScroll, $timeout) {
      $scope.gotoTop = '';
        function location() {
            if ($location.path() == '/' || $location.path() == '/search') {
                $rootScope.home = true;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
            } else if ($location.path().indexOf('/artiste') > -1){
                $rootScope.pathArt = true;
                $rootScope.home = false;
                $rootScope.pathEvent = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
            } else if ($location.path().indexOf('/event') > -1){
                rootScope.pathEvent = true;
                $rootScope.pathArt = false;
                $rootScope.home = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
            } else if ($location.path().indexOf('/user') > -1){
                $rootScope.pathUsr = true;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.home = false;
                $rootScope.pathPlace = false;
            } else if ($location.path().indexOf('/lieu') > -1){
                $rootScope.pathUsr = false;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.home = false;
                $rootScope.pathPlace = true;
            }
        }
        $scope.$on('$locationChangeSuccess', function(){
            location()
        });
        $rootScope.activArtist = false;
        $rootScope.activEvent = true;
        $rootScope.activPlace = false;
        $rootScope.activUsr = false;
      $scope.$on("$routeChangeSuccess", function(event, next, current){
          $timeout(function(){
              $scope.gotoTop = function() {
                // set the location.hash to the id of
                // the element you wish to scroll to.
                $location.hash('top');
                // call $anchorScroll()
                $anchorScroll();
              };
          }, 200);
      });

    }

]);

