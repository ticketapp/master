app.controller ('lecteurCtrl', ['$scope', '$rootScope', '$timeout', '$http', function ($scope, $rootScope, $timeout, $http){
    $rootScope.playlist = [];
    $scope.showLecteur = true;
    $scope.playPause = "";
    $scope.onPlay = false;
    var i = 0;
    function pushTrack (el) {
        $rootScope.playlist.push(el);
    }
    $rootScope.addToPlaylist = function (tracks) {
        if ($rootScope.playlist.length == 0) {
            tracks.forEach(pushTrack);
            $scope.play(i)
        } else {
            tracks.forEach(pushTrack);
        }
    };
    $rootScope.addAndPlay = function (tracks) {
        var last = $rootScope.playlist.length;
        tracks.forEach(pushTrack);
        $scope.play(last);
        console.log($rootScope.playlist)
    };
    $scope.play = function (i) {
        if (typeof(updateProgressYt) != "undefined") {
            clearInterval(updateProgressYt);
        }
        function nextSoundT () {
            i++;
            $scope.play(i);
        }
        function updateProgress() {
            var progress = document.getElementById("progress");
            var value = 0;
            if (document.getElementById('musicPlayer').currentTime > 0) {
                value = 100 / document.getElementById('musicPlayer').duration * document.getElementById('musicPlayer').currentTime;
            }
            progress.style.width = value + "%";
            var ct = new Date(document.getElementById('musicPlayer').currentTime*1000);
            var durat = new Date(document.getElementById('musicPlayer').duration*1000);
            document.getElementById('currentTime').innerHTML = ct.getMinutes() + ':' + ct.getSeconds() +
                ' / ' + durat.getMinutes() + ':' + durat.getSeconds();
        }
        $scope.closeTrack = function (index) {
            $rootScope.playlist.splice(index, 1);
            if (index == $rootScope.playlist.length && index == i) {
                document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
                document.getElementById('musicPlayer').removeEventListener('ended', nextSoundT);
                document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
            }
        };
        $scope.remPlaylist = function () {
            $rootScope.playlist = [];
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
            document.getElementById('musicPlayer').removeEventListener('ended', nextSoundT);
            document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
        };
        document.getElementById('musicPlayer').removeEventListener('ended', nextSoundT);
        document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
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
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
            document.getElementById('musicPlayer').setAttribute('src', $rootScope.playlist[i].url + '?client_id=f297807e1780623645f8f858637d4abb');
            $scope.onPlay = true;
            $scope.playPause = function () {
                if ($scope.onPlay == false) {
                    document.getElementById('musicPlayer').play();
                    $scope.onPlay = true;
                } else {
                    document.getElementById('musicPlayer').pause();
                    $scope.onPlay = false;
                }
            };
            document.getElementById('musicPlayer').addEventListener("timeupdate", updateProgress);
            if (i > 0) {
                function goToTrackActive () {
                    if (document.getElementsByClassName('trackContener').length >= i) {
                        $timeout(function() {
                            var posTrackActive = document.getElementsByClassName('trackContener')[i].getBoundingClientRect();
                            document.getElementsByClassName('playlistScroller')[0].scrollLeft = document.getElementsByClassName('playlistScroller')[0].scrollLeft + posTrackActive.left - 5;
                        }, 100);
                    } else {
                        goToTrackActive ()
                    }
                }
                goToTrackActive ()
            }
        } else if ($rootScope.playlist[i].from == 'youtube') {
            document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
            //document.getElementById('musicPlayer').classList.add('ng-hide');
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer' class='ng-hide'></div>";
            //document.getElementById('youtubePlayer').classList.remove('ng-hide');
            document.getElementById('youtubePlayer').setAttribute('src', $rootScope.playlist[i].url);
            function onPlayerReady(event) {
                event.target.playVideo();
                $scope.onPlay = true;
                $scope.playPause = function () {
                    if ($scope.onPlay == false) {
                        event.target.playVideo();
                        $scope.onPlay = true;
                    } else {
                        event.target.pauseVideo();
                        $scope.onPlay = false;
                    }
                };
                if (i > 0) {
                    function goToTrackActive () {
                        if (document.getElementsByClassName('trackContener').length >= i) {
                            $timeout(function() {
                                var posTrackActive = document.getElementsByClassName('trackContener')[i].getBoundingClientRect();
                                document.getElementsByClassName('playlistScroller')[0].scrollLeft = document.getElementsByClassName('playlistScroller')[0].scrollLeft + posTrackActive.left - 5;
                            }, 100);
                        } else {
                            goToTrackActive ()
                        }
                    }
                    goToTrackActive ()
                }
            }
            function onPlayerStateChange(event) {
                if (event.data === 1) {
                    var duration = event.target.getDuration();
                    var curentDuration = event.target.getCurrentTime();
                    updateProgressYt = setInterval(function () {
                        curentDuration = event.target.getCurrentTime();
                        var prog = document.getElementById("progress");
                        var val = 0;
                        if (curentDuration > 0) {
                            val = 100 / duration * curentDuration;
                        }
                        prog.style.width = val + "%";
                        var cty = new Date(curentDuration*1000);
                        var duraty = new Date(duration*1000);
                        document.getElementById('currentTime').innerHTML = cty.getMinutes() + ':' + cty.getSeconds() +
                            ' / ' + duraty.getMinutes() + ':' + duraty.getSeconds();
                        if (val == 100) {
                            if (typeof(updateProgressYt) != "undefined") {
                                clearInterval(updateProgressYt);
                            }
                        }
                    }, 100);
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
        document.getElementById('musicPlayer').addEventListener('ended', nextSoundT);
    };
}]);