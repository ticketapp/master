app.controller ('lecteurCtrl', ['$scope', '$rootScope', '$timeout', '$http', 'Angularytics', function ($scope, $rootScope, $timeout, $http, Angularytics){
    $rootScope.playlist = [];
    $rootScope.playlist.name = "";
    $rootScope.playlist.by = "";
    $rootScope.playlist.tracks = [];
    $scope.showLecteur = true;
    $scope.playPause = "";
    $scope.onPlay = false;
    $scope.levelVol = 100;
    $scope.shuffle = false;
    $scope.sortableOptions = {
        containment: '#sortable-container',
        //restrict move across columns. move only within column.
        accept: function (sourceItemHandleScope, destSortableScope) {
            return sourceItemHandleScope.itemScope.sortableScope.$id === destSortableScope.$id;
        }
    };
    var played = [];
    var i = 0;
    function pushTrack (track, art) {
        $scope.newTrack = {};
        $scope.newTrack.platform = track.platform;
        if (track.thumbnailUrl != undefined) {
            $scope.newTrack.thumbnailUrl = track.thumbnailUrl;
        } else if (track.avatarUrl != undefined) {
            $scope.newTrack.avatarUrl = track.avatarUrl;
        } else if (track.thumbnail != undefined) {
            $scope.newTrack.thumbnail = track.thumbnail;
        }
        $scope.newTrack.url = track.url;
        $scope.newTrack.artist = art;
        $scope.newTrack.title = track.title.replace(art + " - ", "");
        $scope.newTrack.title = $scope.newTrack.title.replace(art + "-", "");
        $scope.newTrack.title = $scope.newTrack.title.replace(art.toLowerCase() + " - ", "");
        $scope.newTrack.title = $scope.newTrack.title.replace(art.toLowerCase() + "-", "");
        $scope.newTrack.title = $scope.newTrack.title.replace(art.toUpperCase() + " - ", "");
        $scope.newTrack.title = $scope.newTrack.title.replace(art.toUpperCase() + "-", "");
        $rootScope.playlist.tracks.push($scope.newTrack);
        console.log($scope.newTrack)
        console.log($rootScope.playlist.tracks)
    }
    $rootScope.addToPlaylist = function (tracks, artist) {
        if ($rootScope.playlist.tracks.length == 0) {
            var tracksLenght = tracks.length;
            for (var tr = 0; tr < tracksLenght; tr++) {
                pushTrack(tracks[tr], artist)
            }
            $scope.play(i);
            played = [];
        } else {
            var tracksLenght = tracks.length;
            for (var tr = 0; tr < tracksLenght; tr++) {
                pushTrack(tracks[tr], artist)
            }
        }
    };
    $rootScope.addAndPlay = function (tracks, artist) {
        var last = $rootScope.playlist.tracks.length;
        var tracksLenght = tracks.length;
        for (var tr = 0; tr < tracksLenght; tr++) {
            pushTrack(tracks[tr], artist)
        }
        //Angularytics.trackEvent("listen music", artist);
        $scope.play(last);
        if ($rootScope.playlist.tracks.length == 0) {
            played = [];
        }
    };
    function alreadyPlayed (element, index, array) {
        return played.indexOf(element.title) > -1;
    }
    function shuffle () {
        if ( i > $rootScope.playlist.tracks.length - 1) {
            i = (Math.floor((Math.random() * $rootScope.playlist.tracks.length) + 1));
            shuffle()
        } else if (played.indexOf($rootScope.playlist.tracks[i].title) > -1) {
            if ($rootScope.playlist.tracks.every(alreadyPlayed) == true) {
                console.log('all tracks already played')
            } else {
                i = (Math.floor((Math.random() * $rootScope.playlist.tracks.length) + 1));
                shuffle()
            }
        } else {
            $scope.play(i)
        }
    }
    function readableDuration(seconds) {
        sec = Math.floor( seconds );
        min = Math.floor( sec / 60 );
        min = min >= 10 ? min : '0' + min;
        sec = Math.floor( sec % 60 );
        sec = sec >= 10 ? sec : '0' + sec;
        if (min >= 60) {
            hr = Math.floor(min / 60);
            min = Math.floor(min % 60);
            min = min >= 10 ? min : '0' + min;
            return hr + ':' + min + ':' + sec;
        } else {
            return min + ':' + sec;
        }
    }
    $scope.play = function (i) {
        $scope.prevTrack = function () {
            i--;
            $scope.play(i);
        };
        $scope.nextTrack = function () {
            if ($scope.shuffle == true) {
                i = (Math.floor((Math.random() * $rootScope.playlist.tracks.length) + 1));
                shuffle()
            } else {
                i++;
                $scope.play(i);
            }
        };
        played.push($rootScope.playlist.tracks[i].title);
        if (typeof(updateProgressYt) != "undefined") {
            clearInterval(updateProgressYt);
        }
        function updateProgress() {
            var progress = document.getElementById("progress");
            var value = 0;
            if (document.getElementById('musicPlayer').currentTime > 0) {
                value = 100 / document.getElementById('musicPlayer').duration * document.getElementById('musicPlayer').currentTime;
            }
            progress.style.width = value + "%";
            document.getElementById('currentTime').innerHTML =
                readableDuration(document.getElementById('musicPlayer').currentTime) +
                ' / ' + readableDuration(document.getElementById('musicPlayer').duration);
        }
        $scope.closeTrack = function (index) {
            $rootScope.playlist.tracks.splice(index, 1);
            if (index == $rootScope.playlist.tracks.length && index == i) {
                document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
                document.getElementById('musicPlayer').removeEventListener('ended', $scope.next);
                document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
            }
        };
        $scope.remPlaylist = function () {
            $rootScope.playlist.tracks = [];
            played = [];
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
            document.getElementById('musicPlayer').removeEventListener('ended', $scope.next);
            document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
        };
        document.getElementById('musicPlayer').removeEventListener('ended', $scope.next);
        document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
        $scope.trackActive = $rootScope.playlist.tracks[i];
        if ($rootScope.playlist.tracks[i].platform == 'Soundcloud') {
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
            if ($rootScope.playlist.tracks[i].url != undefined) {
                document.getElementById('musicPlayer').setAttribute('src', $rootScope.playlist.tracks[i].url + '?client_id=f297807e1780623645f8f858637d4abb');
            } else {
                $scope.nextTrack();
            }
            $scope.onPlay = true;
            $scope.$apply();
            $scope.playPause = function () {
                if ($scope.onPlay == false) {
                    document.getElementById('musicPlayer').play();
                    $scope.onPlay = true;
                    $scope.$apply();
                } else {
                    document.getElementById('musicPlayer').pause();
                    $scope.onPlay = false;
                }
            };
            $scope.volume = function () {
                document.getElementById('musicPlayer').volume = $scope.levelVol/100
            };
            document.getElementById("progressBar").onclick = function (event) {
                document.getElementById('musicPlayer').currentTime =
                    document.getElementById('musicPlayer').duration * ((event.clientX - document.getElementById("progressBar").getBoundingClientRect().left)
                    / document.getElementById("progressBar").clientWidth)
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
        } else if ($rootScope.playlist.tracks[i].platform == 'Youtube') {
            document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
            //document.getElementById('musicPlayer').classList.add('ng-hide');
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            //document.getElementById('youtubePlayer').classList.remove('ng-hide');
            document.getElementById('youtubePlayer').setAttribute('src', $rootScope.playlist.tracks[i].url);
            function onPlayerReady(event) {
                console.log('youtube')
                event.target.playVideo();
                $scope.onPlay = true;
                $scope.$apply();
                $scope.playPause = function () {
                    if ($scope.onPlay == false) {
                        event.target.playVideo();
                        $scope.onPlay = true;
                        $scope.$apply();
                    } else {
                        event.target.pauseVideo();
                        $scope.onPlay = false;
                    }
                };
                var yPlayer = event.target;
                $scope.volume = function () {
                    yPlayer.setVolume($scope.levelVol);
                };
                document.getElementById("progressBar").onclick = function (event) {
                    var newPos = yPlayer.getDuration() * ((event.clientX - document.getElementById("progressBar").getBoundingClientRect().left)
                        / document.getElementById("progressBar").clientWidth);
                    yPlayer.seekTo(newPos, true);
                    $scope.onPlay = true;
                    $scope.$apply();
                    yPlayer.playVideo();
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
                        document.getElementById('currentTime').innerHTML =
                            readableDuration(curentDuration) +
                            ' / ' + readableDuration(duration);
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
                    $scope.nextTrack()
                }
            }
            var player = new YT.Player('youtubePlayer', {
                height: '200',
                width: '100%',
                videoId: $rootScope.playlist.tracks[i].url,
                events: {
                    'onReady': onPlayerReady,
                    'onStateChange': onPlayerStateChange,
                    'onError' : function () {$scope.nextTrack()}
                }
            });
        }
        document.getElementById('musicPlayer').addEventListener('ended', $scope.nextTrack);
    };
    /*$scope.savePlaylist = function () {
        $http.post('/playlist',
        )
    }*/
}]);