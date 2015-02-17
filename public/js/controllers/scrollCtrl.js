app.controller('scrollCtrl', ['$scope','$rootScope', '$location', '$timeout', '$anchorScroll', '$http',
    function ($scope, $rootScope, $location, $anchorScroll, $timeout, $http) {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function(position)
                {
                    //alert("Latitude : " + position.coords.latitude + ", longitude : " + position.coords.longitude);
                }, function erreurPosition(error) {
                }
            );
        } else {
        }
        function eventListener () {
            if (window.pageYOffset < 50) {
                document.getElementById('eventInfo').style.marginTop = '0';
                document.getElementById('generalControlsContener').style.marginRight = '0';
                document.getElementById('showInfo').classList.add('ng-hide');
            } else {
                document.getElementById('eventInfo').style.marginTop = '-100px';
                document.getElementById('generalControlsContener').style.marginRight = '-60px';
                document.getElementById('showInfo').classList.remove('ng-hide');
            }
        }
        function otherListener () {
            if (window.pageYOffset < 50) {
                document.getElementById('generalControlsContener').style.marginRight = '0';
            } else {
                document.getElementById('generalControlsContener').style.marginRight = '-60px';
            }
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
                window.removeEventListener("scroll", eventListener);
                window.addEventListener("scroll", otherListener);
            } else if ($location.path() == '/search'){
                $rootScope.pathArt = false;
                $rootScope.home = false;
                $rootScope.pathEvent = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = true;
                window.removeEventListener("scroll", eventListener);
                window.addEventListener("scroll", otherListener);
            } else if ($location.path().indexOf('/artiste') > -1){
                $rootScope.pathArt = true;
                $rootScope.home = false;
                $rootScope.pathEvent = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
                window.removeEventListener("scroll", eventListener);
                window.addEventListener("scroll", otherListener);
            } else if ($location.path().indexOf('/event') > -1){
                $rootScope.pathEvent = true;
                $rootScope.pathArt = false;
                $rootScope.home = false;
                $rootScope.pathUsr = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
                matched = $location.path().match(/\d.*/);
                $http.get('/events/' + matched[0])
                    .success(function(data, status){
                        $scope.event = data;
                        console.log(data);
                    }).error(function(data, status){
                        console.log(data);
                    });
                $scope.showInfo = function () {
                    document.getElementById('eventInfo').style.marginTop = '0';
                    document.getElementById('showInfo').classList.add('ng-hide');
                };
                window.removeEventListener("scroll", otherListener);
                window.addEventListener("scroll", eventListener);
            } else if ($location.path().indexOf('/user') > -1){
                $rootScope.pathUsr = true;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.home = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
                window.removeEventListener("scroll", eventListener);
                window.addEventListener("scroll", otherListener);
            } else if ($location.path().indexOf('/lieu') > -1){
                $rootScope.pathUsr = false;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.home = false;
                $rootScope.pathPlace = true;
                $rootScope.pathSearch = false;
                window.removeEventListener("scroll", eventListener);
                window.addEventListener("scroll", otherListener);
            } else {
                $rootScope.pathUsr = false;
                $rootScope.pathArt = false;
                $rootScope.pathEvent = false;
                $rootScope.home = false;
                $rootScope.pathPlace = false;
                $rootScope.pathSearch = false;
                window.removeEventListener("scroll", eventListener);
                window.addEventListener("scroll", otherListener);
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

