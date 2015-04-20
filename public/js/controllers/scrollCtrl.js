app.filter('millSecondsToTimeString', function() {
    return function(millseconds) {
        var seconds = Math.floor(millseconds*3600000 / 1000);
        var days = Math.floor(seconds / 86400);
        var hours = Math.floor((seconds % 86400) / 3600);
        var months = Math.floor(days / 30);
        var timeString = '';
        
        if(months > 0) timeString += (months > 1) ? (months + " mois ") : (months + " mois ");
        if(days > 0 && months == 0) timeString += (days > 1) ? (days + " jours ") : (days + " jours ");
        if(hours > 0) timeString += (hours > 1) ? (hours + " heures ") : (hours + " heure ");
        return timeString;
    }
});
app.controller('scrollCtrl', ['$scope','$rootScope', '$location', '$timeout', '$anchorScroll', '$http', 'Angularytics', '$websocket', 'oboe', '$modal', '$log', '$filter',
    function ($scope, $rootScope, $location, $timeout, $anchorScroll, $http, Angularytics, $websocket, oboe, $modal, $log, $filter) {
        $rootScope.geoLoc = '';
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function (position) {
                    $rootScope.geoLoc = "(" + position.coords.latitude + "," + position.coords.longitude + ")";
                    $rootScope.$apply();
                }, function erreurPosition(error) {
                }
            );
        }
        /*else {
         }*/
        /*$http.get('http://www.corsproxy.com/youtube.com/get_video_info?video_id=X8pBHM9u4ko').
         success(function (data) {
         
         })*/
        $scope.needConnect = false;
        $rootScope.lastReq = {};
        $rootScope.follow = function (route, id, name) {
            $http.post('/'+ route +'/'+ id + '/follow').
                success(function (data) {
                    $scope.info = 'vous suivez maintenant ' + name;
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
                }).
                error(function (data) {
                    if (data.error == 'Credentials required') {
                        $rootScope.storeLastReq('post', '/events/'+ id + '/follow', '', 'vous suivez maintenant ' + name)
                    } else {
                        $scope.info = 'Désolé une erreur s\'est produite';
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
                })
        };
        $rootScope.passConnectedToFalse = function () {
            $rootScope.connected = false;
        };
        $rootScope.passConnectedToTrue = function () {
            $timeout(function () {
                $rootScope.$apply(function() {
                    $rootScope.connected = true;
                    $scope.needConnect = false;
                })
            }, 0)
        };
        $rootScope.passArtisteToCreateToFalse = function () {
            $rootScope.artisteToCreate = false;
        };
        $rootScope.storeLastReq = function (method, path, object, success, error) {
            //$scope.needConnect = true;
            var modalInstance = $modal.open({
                templateUrl: 'assets/partials/connectionModal.html',
                controller: 'ModalInstanceCtrl',
                resolve: {
                    items: function () {
                        return $scope.items;
                    },
                    connected: function () {
                        return $scope.connected;
                    }
                }
            });
            modalInstance.result.then(function (selectedItem) {
                $scope.selected = selectedItem;
            }, function () {
                $log.info('Modal dismissed at: ' + new Date());
            });
            $rootScope.lastReq = {
                'method': method, 'path': path, 'object':object, 'success':success, 'error': error
            };

        };
        $rootScope.passArtisteToCreateToFalse();
        $rootScope.createArtist = function (artist) {
            $rootScope.loadingTracks = true;
            $rootScope.artisteToCreate = true;
            $rootScope.artiste = artist;
            $rootScope.artiste.events = [];
            $rootScope.tracks = [];
            window.location.href =('#/artiste/' + artist.facebookUrl);
            $rootScope.$apply;
            var searchPattern  = document.getElementById('searchBar').value.trim();
            oboe.post('artists/createArtist', {
                searchPattern: searchPattern,
                artist: {
                    facebookUrl: artist.facebookUrl,
                    artistName: artist.name,
                    facebookId: artist.facebookId,
                    imagePath: artist.imagePath,
                    websites: artist.websites,
                    description: artist.description,
                    genre: artist.genre
                }
            }).start(function (data, etc) {
                $rootScope.loadingTracks = true;
            })
            .done(function (value) {
                $timeout(function () {
                    $rootScope.$apply(function () {
                        $rootScope.artiste.tracks = $rootScope.artiste.tracks.concat(value);
                        $rootScope.tracks = $rootScope.artiste.tracks;
                    });
                    if (value.length > 0) {
                        $rootScope.loadingTracks = false;
                    } else {
                        $timeout(function () {
                          $rootScope.loadingTracks = false;
                        }, 2000)
                    }
                });
                function saveTrack (track) {
                    if (track.redirectUrl == undefined) {
                        track.redirectUrl = track.url;
                    }
                    $http.post('/tracks/create', {
                        artistFacebookUrl: artist.facebookUrl,
                        redirectUrl : track.redirectUrl,
                        title: track.title,
                        url: track.url,
                        platform: track.platform,
                        thumbnailUrl: track.thumbnailUrl
                    }).error(function (data) {
                    })
                }
                value.forEach(saveTrack)

            })
            .fail(function (error) {
                    console.log(error)
            });
        };
        $rootScope.resizePageElementsWithEvents = function() {

        };
        function resizeArtistsText (ImgHeight) {
            var artists = document.getElementsByClassName('minNoText');
            for (var i = 0; i < artists.length; i++) {
                var newTextHeight = 94 + (ImgHeight/2);
                artists[i].style.height = newTextHeight + 'px';
            }
        }
        $rootScope.resizeImgHeight = function () {
            var waitForContentMin = setTimeout(function () {
                var content = document.getElementsByClassName('img_min_evnt');
                if (content.length > 0) {
                    clearInterval(waitForContentMin);
                    var newImgHeight = content[0].clientWidth * 0.376;
                    for (var i = 0; i < content.length; i++) {
                        if (content[i].clientWidth > 0) {
                            newImgHeight = content[i].clientWidth * 0.376;
                        }
                        content[i].style.height = newImgHeight + 'px';
                        if (i == content.length - 1) {
                            resizeArtistsText(newImgHeight)
                        }
                    }
                }
            }, 100)
        };
        $rootScope.window = 'large';
        $rootScope.marginContent = function() {
            if ($rootScope.path != 'home') {
                var waitForContentParallax = setTimeout(function () {
                    var content = document.getElementsByClassName('parallax-content');
                    if (content.length > 0) {
                        clearInterval(waitForContentParallax);
                        var contentLength = content.length;
                        for (var i = 0; i < contentLength; i++) {
                            content[i].style.marginTop = window.innerWidth * 0.376 + 'px';
                        }
                    }
                }, 100)
            } else {
                var waitForContentParallax = setTimeout(function () {
                    var content = document.getElementsByClassName('parallax-content');
                    if (content.length > 0) {
                        clearInterval(waitForContentParallax);
                        var contentLength = content.length;
                        for (var i = 0; i < contentLength; i++) {
                            content[i].style.marginTop = '500px';
                        }
                    }
                }, 100)
            }
        }
        function respClass () {
            $rootScope.resizeImgHeight();
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
            $rootScope.marginContent();
        }
        $timeout(function () {
            respClass ();
        }, 0);
        window.onresize = respClass;
        angular.element(document).ready($rootScope.marginContent());
        function fixControl () {
            var titlePos = document.getElementById('eventTitle').getBoundingClientRect();
            if (document.getElementById('wysiwygControl').getBoundingClientRect().top <= 0) {
                document.getElementById('wysiwygControl').style.position = 'fixed';
                document.getElementById('wysiwygControl').style.top = 0;
            }
            if (titlePos.bottom >= 0) {
                document.getElementById('wysiwygControl').style.position = 'relative';
            }
        }
        $scope.gotoTop = '';
        function location() {
            if ($location.path() == '/') {
                $rootScope.path = 'home';
            } else if ($location.path() == '/search'){
                $rootScope.path = 'search';
            } else if ($location.path().indexOf('/artiste') > -1){
                $rootScope.path = 'art';
            } else if ($location.path().indexOf('/event') > -1){
                $rootScope.path = 'event';
            } else if ($location.path().indexOf('/user') > -1){
                $rootScope.path = 'usr';
            } else if ($location.path().indexOf('/lieu') > -1){
                $rootScope.path = 'place';
            }else if ($location.path().indexOf('/iframeEvents') > -1){
                $rootScope.path = 'iframe';
            } else {
                $rootScope.path = false;
            }
            if ($location.path().indexOf('/createEvent') > -1) {
                window.addEventListener('scroll', fixControl)
            } else {
                window.removeEventListener('scroll', fixControl)
            }
        }
        $scope.$on('$locationChangeSuccess', function() {
            location();
        });
        $rootScope.activArtist = false;
        $rootScope.activEvent = true;
        $rootScope.activPlace = false;
        $rootScope.activUsr = false;
        $rootScope.maxStart = 30;
        $rootScope.maxStartView = 168;
        $rootScope.redirectToSearch = function (research) {
            $rootScope.storeSearch = research
            if ($location.path() != '/') {
                $location.path('/search')
            }
        };
        $rootScope.remStoreSearch = function () {
            $rootScope.storeSearch = '';
        }
    }
]);
app.controller('infoModalCtrl', function ($scope, $rootScope, $modalInstance, $http, info) {
    $scope.info = info;
    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});

