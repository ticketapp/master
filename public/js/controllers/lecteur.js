app.controller ('lecteurCtrl', ['$scope', '$rootScope', '$timeout', '$http', function ($scope, $rootScope, $timeout, $http){
    $rootScope.playlist = [];
    $scope.showLecteur = true;
    $scope.playPause = "";
    var play = false;
    var i = 0;
    function pushTrack (el) {
        $rootScope.playlist.push(el);
    }
    $rootScope.addToPlaylist = function (tracks) {
        if ($rootScope.playlist.length == 0) {
            document.getElementById('contener').style.paddingBottom = '400px';
            tracks.forEach(pushTrack);
            $scope.play(i)
        } else {
            tracks.forEach(pushTrack);
        }
    };
    $rootScope.addAndPlay = function (tracks) {
        var last = $rootScope.playlist.length;
        tracks.forEach(pushTrack);
        $scope.play(last)
        console.log($rootScope.playlist)
    };
    $scope.closeTrack = function (index) {
        $rootScope.playlist.splice(index, 1);
    };
    $scope.play = function (i) {
        if (typeof(updateProgressYt) != "undefined") {
            clearInterval(updateProgressYt);
        }
        $scope.trackActive = i;
        $scope.prevTrack = function () {
            i--;
            $scope.play(i);
        };
        $scope.nextTrack = function () {
            i++;
            $scope.play(i);
        };
        if ($rootScope.playlist[i].from == 'soundcloud') {
            //document.getElementById('youtubePlayer').classList.add('ng-hide');
            //document.getElementById('musicPlayer').classList.remove('ng-hide');
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            document.getElementById('musicPlayer').removeAttribute('src');
            document.getElementById('musicPlayer').setAttribute('src', $rootScope.playlist[i].url + '?client_id=f297807e1780623645f8f858637d4abb');
            play = true;
            $scope.playPause = function () {
                if (play == false) {
                    document.getElementById('musicPlayer').play();
                    play = true;
                } else {
                    document.getElementById('musicPlayer').pause();
                    play = false;
                }
            };
            function updateProgress() {
                var progress = document.getElementById("progress");
                var value = 0;
                console.log(document.getElementById('musicPlayer').duration);
                console.log(document.getElementById('musicPlayer').currentTime);
                if (document.getElementById('musicPlayer').currentTime > 0) {
                    value = 100 / document.getElementById('musicPlayer').duration * document.getElementById('musicPlayer').currentTime;
                    console.log(value)
                }
                progress.style.width = value + "%";
                console.log(value)
            }
            document.getElementById('musicPlayer').addEventListener("timeupdate", updateProgress);
            if (i > 0) {
                function goToTrackActive () {
                    if (document.getElementsByClassName('trackContener').length >= i) {
                        $timeout(function() {
                            var posTrackActive = document.getElementsByClassName('trackContener')[i].getBoundingClientRect();
                            document.getElementsByClassName('playlistScroller')[0].scrollLeft = document.getElementsByClassName('playlistScroller')[0].scrollLeft + posTrackActive.left;
                        }, 100);
                    } else {
                        goToTrackActive ()
                    }
                }
                goToTrackActive ()
            }
        } else if ($rootScope.playlist[i].from == 'youtube') {
            document.getElementById('musicPlayer').pause();
            //document.getElementById('musicPlayer').classList.add('ng-hide');
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer' class='ng-hide'></div>";
            //document.getElementById('youtubePlayer').classList.remove('ng-hide');
            document.getElementById('youtubePlayer').setAttribute('src', $rootScope.playlist[i].url);
            function onPlayerReady(event) {
                event.target.playVideo();
                play = true;
                $scope.playPause = function () {
                    if (play == false) {
                        event.target.playVideo();
                        play = true;
                    } else {
                        event.target.pauseVideo();
                        play = false;
                    }
                };
                if (i > 0) {
                    function goToTrackActive () {
                        if (document.getElementsByClassName('trackContener').length >= i) {
                            $timeout(function() {
                                var posTrackActive = document.getElementsByClassName('trackContener')[i].getBoundingClientRect();
                                document.getElementsByClassName('playlistScroller')[0].scrollLeft = document.getElementsByClassName('playlistScroller')[0].scrollLeft + posTrackActive.left;
                            }, 100);
                        } else {
                            goToTrackActive ()
                        }
                    }
                    goToTrackActive ()
                }
            }

            // when video ends
            function onPlayerStateChange(event) {
                if (event.data === 1) {
                    console.log('yoyo');
                    var duration = event.target.getDuration();
                    var curentDuration = event.target.getCurrentTime();
                    updateProgressYt = setInterval(function () {
                        curentDuration = event.target.getCurrentTime();
                        console.log('yo');
                        var prog = document.getElementById("progress");
                        var val = 0;
                        if (curentDuration > 0) {
                            val = 100 / duration * curentDuration;
                        }
                        prog.style.width = val + "%";
                        if (val == 100) {
                            if (typeof(updateProgressYt) != "undefined") {
                                clearInterval(updateProgressYt);
                            }
                        }
                    }, 10);
                } else {
                    if (typeof(updateProgressYt) != "undefined") {
                        clearInterval(updateProgressYt);
                    }
                }
                if(event.data === 0) {
                    i++;
                    $scope.play(i);
                }
            }
            var player = new YT.Player('youtubePlayer', {
                height: '140',
                width: '220',
                videoId: $rootScope.playlist[i].url,
                events: {
                    'onReady': onPlayerReady,
                    'onStateChange': onPlayerStateChange
                }
            });
        }
        document.getElementById('musicPlayer').addEventListener('ended', function () {
            i++;
            $scope.play(i);
        });
    };
    //play(i)
}]);
