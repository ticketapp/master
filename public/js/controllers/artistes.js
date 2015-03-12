app.controller ('ArtistesCtrl', function ($scope, ArtisteFactory, $routeParams, $http, $rootScope, $websocket){
    $scope.bigTracks = true;
    $scope.trackLimit = 10;
    $scope.heightDesc = '147px';
    $scope.trackTitle = '';
    $scope.allDesc = false;
    $scope.suggestQuery = function (trackName, artistName) {
        console.log(trackName, artistName)
    };
    function resizePageElem () {
        var waitForBinding = setInterval(function () {
            if (document.getElementById('events_contener').innerHTML.length > 0) {
                clearInterval(waitForBinding);
                function passElementTo100() {
                    eventInfoConteners[i].classList.remove('large-4');
                    eventInfoConteners[i].classList.remove('large-8');
                    eventInfoConteners[i].classList.add('large-12');
                    if (eventInfoConteners[i].className.indexOf('tracksContener') > -1) {
                        $scope.bigTracks = false;
                    }
                }
                var eventInfoConteners = document.getElementsByClassName('eventInfo');
                /*if ($scope.orgaEvents.length == 1) {
                 document.getElementsByClassName('descriptionContent')[0].classList.remove('large-8');
                 document.getElementsByClassName('descriptionContent')[0].classList.add('large-4');
                 document.getElementsByClassName('descriptionContent')[0].classList.add('paddingLeft0');
                 document.getElementsByClassName('data-ng-event')[0].classList.add('width100p');
                 document.getElementsByClassName('min_contener')[0].classList.add('padding0');
                 $rootScope.resizeImgHeight();
                 var descPlace = document.getElementsByClassName('descriptionContent')[0].getBoundingClientRect();
                 for (var i = 0; i < eventInfoConteners.length; i++) {
                 eventInfoConteners[i].classList.remove('large-4');
                 eventInfoConteners[i].classList.add('large-8');
                 if (eventInfoConteners[i].offsetLeft < 30) {
                 passElementTo100()
                 }
                 }
                 }
                 } else {*/
                    for (var i = 0; i < eventInfoConteners.length; i++) {
                        passElementTo100()
                    }
                if (document.getElementById('descContent') != null &&
                    document.getElementById('descContent').clientHeight < 147) {
                    $scope.allDesc = true;
                    $scope.$apply;
                } else {
                    $scope.allDesc = false;
                    $scope.$apply;
                }
                //}
            }
        }, 100);
    }
    function watchWindowSize () {
        if ($rootScope.window != 'small' && $rootScope.window != 'medium') {
            resizePageElem()
        } else {
            $rootScope.$watch('window', function (newval) {
                if (newval == 'large' || newval == 'xlarge' || newval == 'xxlarge') {
                    resizePageElem()
                }
            })
        }
    }
    function refactorWebsites () {
        for (var i = 0; i < $scope.artiste.websites.length; i++) {
            $scope.artiste.websites[i] = {url: $scope.artiste.websites[i]};
            if ($scope.artiste.websites[i].url.indexOf('facebook') > -1) {
                $scope.artiste.websites[i].name = 'facebook';
            } else if ($scope.artiste.websites[i].url.indexOf('twitter') > -1) {
                $scope.artiste.websites[i].name = 'twitter';
            } else if ($scope.artiste.websites[i].url.indexOf('soundcloud') > -1) {
                $scope.artiste.websites[i].name = 'soundcloud';
            } else if ($scope.artiste.websites[i].url.indexOf('mixcloud') > -1) {
                $scope.artiste.websites[i].name = 'mixcloud';
            } else {
                $scope.artiste.websites[i].name = 'website';
                $scope.otherWebsite = true;
            }
        }
    }
    if ($rootScope.artisteToCreate == false) {
        $rootScope.loadingTracks = true;
        $http.get('/artists/' + $routeParams.facebookUrl)
            .success(function (data, status) {
                $scope.artiste = data;
                refactorWebsites();
                watchWindowSize();
                console.log(data)
                $rootScope.loadingTracks = false
            }).error(function (data, status) {
            });
    } else {
        refactorWebsites();
        $rootScope.passArtisteToCreateToFalse();
        watchWindowSize();
    }
    $rootScope.resizeImgHeight();
});
