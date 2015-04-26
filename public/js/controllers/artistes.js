app.controller ('ArtistesCtrl', function ($scope, $routeParams, $http, $rootScope, $websocket, $timeout, $filter, $modal){
    $scope.bigTracks = true;
    $scope.trackLimit = 12;
    $scope.heightDesc = '147px';
    $scope.trackTitle = '';
    $scope.showDesc = false;
    $scope.selectedTab = 0;
    $scope.suggestQuery = function (trackTitle, artistName, artistFacebookUrl) {
        $scope.suggest = false;
        if (trackTitle.length > 2) {
            artistName = artistName.toLowerCase().replace('officiel', '');
            artistName = artistName.toLowerCase().replace('official', '');
            artistName = artistName.toLowerCase().replace('music', '');
            artistName = artistName.toLowerCase().replace('musique', '');
            artistName = artistName.toLowerCase().replace('musik', '');
            artistName = artistName.toLowerCase().replace('fanpage', '');
            artistName = artistName.toLowerCase().replace(/[^\w\s].*/, '');
            if (trackTitle.toLowerCase() != 'album' && trackTitle.toLowerCase() != 'albums' &&
                trackTitle.toLowerCase() != 'track' && trackTitle.toLowerCase() != 'tune' &&
                trackTitle.toLowerCase() != 'tunes' && trackTitle.toLowerCase() != 'song' &&
                trackTitle.toLowerCase() != 'audio' && trackTitle.toLowerCase() != 'tracks') {
                $http.get('/tracks/' + artistName + '/' + artistFacebookUrl + '/' + trackTitle).
                    success(function (data) {
                        for (var i = 0; i < data.length; i++) {
                            $scope.artiste.tracks.push(data[i]);
                            $scope.tracks.push(data[i])
                        }
                        if (data.length == 0) {
                            $scope.info = 'Nous n\'avons pas trouvé "' + trackTitle + '"';
                            var modalInstance = $modal.open({
                                templateUrl: 'assets/partials/_infoModal.html',
                                controller: 'infoModalCtrl',
                                resolve: {
                                    info: function () {
                                        return $scope.info;
                                    }
                                }
                            });
                            modalInstance.result.then(function () {
                                $log.info('Modal dismissed at: ' + new Date());
                            });
                        }
                    }).
                    error(function (data) {

                    })
            } else {
                $scope.info = 'Requête "' + trackTitle + '" intérdite';
                var modalInstance = $modal.open({
                    templateUrl: 'assets/partials/_infoModal.html',
                    controller: 'infoModalCtrl',
                    resolve: {
                        info: function () {
                            return $scope.info;
                        }
                    }
                });
                modalInstance.result.then(function () {
                    $log.info('Modal dismissed at: ' + new Date());
                });
            }
        } else {
            $scope.info = 'le nom de la track doit faire plus de deux lettres';
            var modalInstance = $modal.open({
                templateUrl: 'assets/partials/_infoModal.html',
                controller: 'infoModalCtrl',
                resolve: {
                    info: function () {
                        return $scope.info;
                    }
                }
            });
            modalInstance.result.then(function () {
                $log.info('Modal dismissed at: ' + new Date());
            });
        }
    };

    $scope.filterTracks = function () {
        $timeout(function () {
            $scope.$apply(function(){
                $scope.artiste.tracks = $filter('filter')($scope.tracks, {title: $scope.trackTitle})
            })
        }, 0)
    };

    function refactorWebsites () {
        for (var i = 0; i < $scope.artiste.websites.length; i++) {
            $scope.artiste.websites[i] = {url: $scope.artiste.websites[i]};
            if ($scope.artiste.websites[i].url.length > 0) {
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
    }

    $scope.calcResize = function () {
        var trackContener = document.getElementsByClassName('tracksContener')[0];
        trackContener.classList.remove('large-12');
        trackContener.classList.remove('large-8');
        trackContener.classList.add('large-4');
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
                
                $rootScope.loadingTracks = false;
                $http.get('/artists/'+ $routeParams.facebookUrl + '/events ').
                    success(function(data){
                        $scope.artiste.events = [];
                        if (data.length == 0) {
                            $scope.selectedTab = 1;
                        }
                        function pushEvent (el) {
                            el.priceColor = '#2DAAE1';
                            if (el.tariffRange != undefined) {
                                var tariffs = el.tariffRange.split('-');
                                if (tariffs[1] > tariffs[0]) {
                                    el.tariffRange = tariffs[0].replace('.0', '') + ' - ' +
                                        tariffs[1].replace('.0', '') + '€';
                                } else {
                                    el.tariffRange = tariffs[0].replace('.0', '') + '€';
                                }
                                el.priceColor = 'rgb(' + tariffs[0]*2 + ',' + (200 - (tariffs[0]*4 ) )+
                                    ',' + tariffs[0]*4 + ')'
                            }
                            $scope.artiste.events.push(el);
                        }
                        data.forEach(pushEvent)
                        $rootScope.resizeImgHeight();
                    }).
                    error(function(data) {
                        //watchWindowSize();
                    })
            }).error(function (data, status) {
            });
    } else {
        $scope.selectedTab = 1;
        refactorWebsites();
        $rootScope.passArtisteToCreateToFalse();
    }
    $rootScope.resizeImgHeight();
});
