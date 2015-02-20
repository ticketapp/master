app.controller('scrollCtrl', ['$scope','$rootScope', '$location', '$timeout', '$anchorScroll', '$http',
    function ($scope, $rootScope, $location, $timeout, $anchorScroll, $http) {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function(position)
                {
                    //alert("Latitude : " + position.coords.latitude + ", longitude : " + position.coords.longitude);
                }, function erreurPosition(error) {
                }
            );
        } else {
        }
        $rootScope.window = 'large';
        function marginContent () {
            if ($rootScope.home == false) {
                var waitForContentParallax = setTimeout(function () {
                    var content = document.getElementsByClassName('parallax-content');
                    if (content.length > 0) {
                        clearInterval(waitForContentParallax);
                        for (var i = 0; i < content.length; i++) {
                            content[i].style.marginTop = window.innerWidth * 0.376 + 'px';
                        }
                    }
                }, 100)
            }
        }
        function respClass () {
                marginContent();
                if (window.innerWidth > 0 && window.innerWidth <= 640) {
                    $scope.$apply(function () {
                        $rootScope.window = 'small';
                    });
                } else if (window.innerWidth > 640 && window.innerWidth <= 1024) {
                    $scope.$apply(function () {
                        $rootScope.window = 'medium';
                    });
                } else if (window.innerWidth > 1024 && window.innerWidth <= 1440) {
                    $scope.$apply(function () {
                        $rootScope.window = 'large';
                    });
                } else if (window.innerWidth > 1440 && window.innerWidth <= 1920) {
                    $scope.$apply(function () {
                        $rootScope.window = 'xlarge';
                    });
                } else if (window.innerWidth > 1920) {
                    $scope.$apply(function () {
                        $rootScope.window = 'xxlarge';
                    });
                }
        }
        $timeout(function () {
            respClass ();
        }, 0);
        window.onresize = respClass;
        angular.element(document).ready(marginContent());
        function fixControl () {
            var controlPos = document.getElementById('wysiwygControl').getBoundingClientRect();
            var titlePos = document.getElementById('eventTitle').getBoundingClientRect();
            if (controlPos.top <= 0) {
                document.getElementById('wysiwygControl').style.position = 'fixed';
                document.getElementById('wysiwygControl').style.top = 0;
            } if (titlePos.bottom >= 0){
                document.getElementById('wysiwygControl').style.position = 'relative';
                controlPos = document.getElementById('wysiwygControl').getBoundingClientRect();
            }
        }
        $scope.gotoTop = '';
        function location() {
            marginContent();
            $timeout(function(){
                /*$location.hash('top');
                $anchorScroll();*/
                window.scrollTo(0, 0);
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
            if ($location.path().indexOf('/createEvent') > -1){
                window.addEventListener('scroll', fixControl)
            } else {
                window.removeEventListener('scroll', fixControl)
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

