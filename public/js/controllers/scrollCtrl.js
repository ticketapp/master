app.controller('scrollCtrl', ['$scope','$rootScope', '$location', '$timeout', '$anchorScroll', '$http', 'Angularytics', '$websocket', 'oboe',
    function ($scope, $rootScope, $location, $timeout, $anchorScroll, $http, Angularytics, $websocket, oboe) {
        $rootScope.geoLoc = '';
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function (position) {
                    $rootScope.geoLoc = "(" + position.coords.latitude + "," + position.coords.longitude + ")";
                    $rootScope.$apply();
                    console.log($rootScope.geoLoc)
                }, function erreurPosition(error) {
                }
            );
        }
        /*else {
         }*/
        /*$http.get('http://www.corsproxy.com/youtube.com/get_video_info?video_id=X8pBHM9u4ko').
            success(function (data) {
                console.log(data)
            })*/
        $rootScope.passArtisteToCreateToFalse = function () {
            $rootScope.artisteToCreate = false;
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
                    console.log(etc, data);
                })
                /*.node('champ.*', function (value) {
                 $scope.items.push(value);
                 })*/
                .done(function (value) {
                    $rootScope.artiste.tracks = $rootScope.artiste.tracks.concat(value);
                    $rootScope.tracks = $rootScope.artiste.tracks;
                    if (value.constructor === Array) {
                        console.log(value);
                    }
                    $rootScope.$apply();
                    function saveTrack (track) {
                        $rootScope.loadingTracks = false;
                        console.log('yo')

                        if (track.redirectUrl == undefined) {
                            track.redirectUrl = track.url;
                        }
                        console.log('yoyo')
                        $http.post('/tracks/create', {
                            artistFacebookUrl: artist.facebookUrl,
                            redirectUrl : track.redirectUrl,
                            title: track.title,
                            url: track.url,
                            platform: track.platform,
                            thumbnailUrl: track.thumbnailUrl
                        }).error(function (data) {
                            console.log(data)
                        })
                    }
                    value.forEach(saveTrack)

                })
                .fail(function (error) {
                    console.log("Error: ", error);
                });
        };
        $rootScope.resizePageElementsWithEvents = function() {

        };
        function resizeArtistsText (ImgHeight) {
            var artists = document.getElementsByClassName('textArtistMin');
            for (var i = 0; i < artists.length; i++) {
                var newTextHeight = 110.5 - (ImgHeight/2) - 10 + 'px';
                artists[i].style.height = newTextHeight;
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
        function marginContent () {
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
            marginContent();
        }
        $timeout(function () {
            respClass ();
        }, 0);
        window.onresize = respClass;
        angular.element(document).ready(marginContent());
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
            marginContent();
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
    }
]);

