app.controller('scrollCtrl', ['$scope', '$timeout', '$location', '$anchorScroll',
    function ($scope, $location, $anchorScroll, $timeout) {
      $scope.gotoTop = ''
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

