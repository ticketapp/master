app.controller('scrollCtrl', ['$scope','$rootScope', '$location', '$timeout', '$anchorScroll', '$http', 'Angularytics', '$websocket', 'oboe',
    function ($scope, $rootScope, $location, $timeout, $anchorScroll, $http, Angularytics, $websocket, oboe) {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(function(position)
                {
                    //alert("Latitude : " + position.coords.latitude + ", longitude : " + position.coords.longitude);
                }, function erreurPosition(error) {
                }
            );
        } /*else {
        }*/
        $rootScope.passArtisteToCreateToFalse = function () {
            $rootScope.artisteToCreate = false;
            console.log($rootScope.artisteToCreate)
        };
        $rootScope.passArtisteToCreateToFalse();
        $rootScope.createArtist = function (artist) {
            $rootScope.artisteToCreate = true;
            $rootScope.artiste = artist;
            $rootScope.$apply;
            var searchPattern  = document.getElementById('searchBar').value.trim();
            console.log(searchPattern);
            oboe.post('artists/createArtist', {
                searchPattern: searchPattern,
                artist: {
                    facebookUrl: artist.facebookUrl,
                    artistName: artist.name,
                    facebookId: artist.facebookId,
                    images: artist.images,
                    websites: artist.websites,
                    description: artist.description,
                    genre: artist.genre
                }
            }).start(function (data, etc) {
                    console.log(etc, data);
                    window.location.href =('#/artiste/' + artist.facebookUrl);
                })
                /*.node('champ.*', function (value) {
                 $scope.items.push(value);
                 })*/
                .done(function (value) {
                    console.log(value)
                    $rootScope.artiste.tracks = $rootScope.artiste.tracks.concat(value);
                    console.log($rootScope.artiste.tracks)
                    $rootScope.$apply();

                })
                .fail(function (error) {
                    console.log("Error: ", error);
                });
        };
        function resizeArtistsText (ImgHeight) {
            var artists = document.getElementsByClassName('textArtistMin');
            for (var i = 0; i < artists.length; i++) {
                var newTextHeight = 110.5 - (ImgHeight/2) - 10 + 'px';
                console.log(newTextHeight)
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
        }
        $rootScope.window = 'large';
        function marginContent () {
            if ($rootScope.home == false) {
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
            marginContent();
            $timeout(function() {
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
            if ($location.path().indexOf('/createEvent') > -1) {
                window.addEventListener('scroll', fixControl)
            } else {
                window.removeEventListener('scroll', fixControl)
            }
        }
        $scope.$on('$locationChangeSuccess', function() {
            location();
            marginContent();
        });
        $rootScope.activArtist = false;
        $rootScope.activEvent = true;
        $rootScope.activPlace = false;
        $rootScope.activUsr = false;
    }
]);

