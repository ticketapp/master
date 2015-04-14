app.controller ('ArtistesCtrl', function ($scope, ArtisteFactory, $routeParams, $http, $rootScope, $websocket, $timeout, $filter){
    $scope.bigTracks = true;
    $scope.trackLimit = 10;
    $scope.heightDesc = '147px';
    $scope.trackTitle = '';
    $scope.allDesc = false;
    $scope.suggestQuery = function (trackTitle, artistName, artistFacebookUrl) {
        console.log(trackTitle, artistName);
        $scope.suggest = false;
        if (trackTitle.length >= 2) {
            artistName = artistName.toLowerCase().replace('officiel', '');
            artistName = artistName.toLowerCase().replace('official', '');
            artistName = artistName.toLowerCase().replace('music', '');
            artistName = artistName.toLowerCase().replace('musique', '');
            artistName = artistName.toLowerCase().replace('musik', '');
            artistName = artistName.toLowerCase().replace(/[^\w\s].*/, '');
            console.log(artistName)
            $http.get('/tracks/' + artistName + '/' + artistFacebookUrl + '/' + trackTitle).
                success(function (data) {
                    console.log(data)
                    for (var i = 0; i < data.length; i++) {
                        $scope.artiste.tracks.push(data[i]);
                        $scope.tracks.push(data[i])
                    }
                    if (data.length == 0) {
                        alert('Nous n\'avons pas trouvé' + trackTitle)
                    }
                }).
                error(function (data) {
                    console.log(data)
                })
        }
    };
    $scope.filterTracks = function () {
        $timeout(function () {
            $scope.$apply(function(){
                $scope.artiste.tracks = $filter('filter')($scope.tracks, {title: $scope.trackTitle})
            })
        },0)
    };
    function resizePageElem () {
        var waitForBinding = setInterval(function () {
            if (document.getElementsByClassName('data-ng-event').length == $scope.artiste.events.length) {
                clearInterval(waitForBinding);
                function passElementTo100() {
                    eventInfoConteners[i].classList.remove('large-4');
                    eventInfoConteners[i].classList.remove('large-8');
                    eventInfoConteners[i].classList.add('large-12');
                    if (eventInfoConteners[i].className.indexOf('tracksContener') > -1) {
                        $scope.bigTracks = false;
                        $scope.$apply();
                    }
                }
                var eventInfoConteners = document.getElementsByClassName('eventInfo');
                if (document.getElementById('descContent') != null) {
                    if (document.getElementById('descContent').clientHeight < 147) {
                        $scope.heightDesc = '';
                        $scope.$apply();
                    }
                }
                if ($scope.artiste.events.length > 0) {
                    if ($scope.artiste.events.length == 1) {
                        $scope.bigTracks = false;
                        $scope.$apply();
                        document.getElementsByClassName('descriptionContent')[0].classList.remove('large-8');
                        document.getElementsByClassName('descriptionContent')[0].classList.add('large-4');
                        document.getElementsByClassName('descriptionContent')[0].classList.add('paddingLeft0');
                        document.getElementsByClassName('data-ng-event')[0].classList.add('width100p');
                        document.getElementsByClassName('min_contener')[0].classList.add('padding0');
                        $rootScope.resizeImgHeight();
                        for (var i = 0; i < eventInfoConteners.length; i++) {
                            eventInfoConteners[i].classList.remove('large-4');
                            eventInfoConteners[i].classList.add('large-8');
                            if (eventInfoConteners[i].offsetLeft < 30) {
                                passElementTo100()
                            }
                        }
                    }
                    for (var i = 0; i < eventInfoConteners.length; i++) {
                        if (eventInfoConteners[i].offsetLeft < 30) {
                            passElementTo100()
                        }
                    }
                    $rootScope.resizeImgHeight();
                } else {
                    for (var i = 0; i < eventInfoConteners.length; i++) {
                        passElementTo100()
                    }
                    if (document.getElementById('descContent') != null &&
                        document.getElementById('descContent').clientHeight < 147) {
                        $scope.allDesc = true;
                        $scope.$apply();
                    } else {
                        $scope.allDesc = false;
                        $scope.$apply();
                    }
                    $rootScope.resizeImgHeight();
                }
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
    $scope.calcResize = function () {
        var trackContener = document.getElementsByClassName('tracksContener')[0];
        trackContener.classList.remove('large-12');
        trackContener.classList.remove('large-8');
        trackContener.classList.add('large-4');
        watchWindowSize();
    };
    if ($rootScope.artisteToCreate == false) {
        $scope.artiste = [];
        $scope.artiste.events = [];
        $rootScope.loadingTracks = true;
        $http.get('/artists/' + $routeParams.facebookUrl)
            .success(function (data, status) {
                $rootScope.marginContent();
                $scope.artiste = data;
                $scope.tracks = $scope.artiste.tracks;
                if ($scope.artiste.websites != undefined) {
                    refactorWebsites();
                }
                console.log(data)
                $rootScope.loadingTracks = false;
                $http.get('/artists/'+ $routeParams.facebookUrl + '/events ').
                    success(function(data){
                        $scope.artiste.events = data;
                        watchWindowSize();
                    }).
                    error(function(data) {
                        //watchWindowSize();
                    })
            }).error(function (data, status) {
            });
    } else {
        refactorWebsites();
        $rootScope.passArtisteToCreateToFalse();
        watchWindowSize();
    }
    $rootScope.resizeImgHeight();
});
