app.controller ('lecteurCtrl', ['$scope', '$rootScope', '$timeout', '$http', 'Angularytics', function ($scope, $rootScope, $timeout, $http, Angularytics){
    $rootScope.playlist = [];
    $rootScope.playlist.name = "";
    $rootScope.playlist.genres = [];
    $rootScope.playlist.by = "";
    $rootScope.playlist.tracks = [];
    $scope.showLecteur = true;
    $scope.playPause = "";
    $scope.onPlay = false;
    $scope.levelVol = 100;
    $scope.shuffle = false;
    $scope.playlistEnd = false;
    var offset = 0;
    $scope.sortableOptions = {
        containment: '#sortable-container',
        //restrict move across columns. move only within column.
        accept: function (sourceItemHandleScope, destSortableScope) {
            return sourceItemHandleScope.itemScope.sortableScope.$id === destSortableScope.$id;
        }
    };
    var playlistEvents = [];
    var played = [];
    var artNames = [];
    var i = 0;
    $scope.createNewPlaylist = function (playlist) {
        var tracksToSave = [];
        console.log(playlist)
        for (var i=0; i < playlist.tracks.length; i++) {
            tracksToSave.push({trackId: playlist.tracks[i].trackId})
        }
        console.log(tracksToSave);
        $http.post('/playlists/create', {name: playlist.name, tracksId: tracksToSave}).
            success(function (data) {
                console.log(data)
            }).
            error(function (data) {
                console.log(data)
                var object = {name: playlist.name , tracksId: tracksToSave};
                $rootScope.storeLastReq('post', '/playlists/create', object, 'votre playlist'+ playlist.name + 'est enregistée')
            })
    };
    function eventsPlaylist () {
        $http.get('/events/offset/' + offset +'/' + $rootScope.geoLoc).
            success(function(data){
                function getEventsArtsts (event) {
                   var evArtLenght = event.artists.length
                    for (var a = 0; a < evArtLenght; a++) {
                        console.log(artNames)
                        for(var g = 0; g < event.artists[a].genres.length; g++) {
                            /*console.log(event.artists[a].genres[g].name)
                            console.log(event.artists[a])
                            console.log($rootScope.playlist.genres)*/
                            if ($rootScope.playlist.genres.toString().toLowerCase().indexOf(event.artists[a].genres[g].name.toLowerCase()) > -1 && artNames.toString().indexOf(event.artists[a].name) == -1) {
                                artNames.push(event.artists[a].name);
                                for (var t = 0; t < 4; t++) {
                                    if (played.indexOf(event.artists[a].tracks[t].title) == -1) {
                                        event.artists[a].tracks[t].art = event.artists[a];
                                        event.artists[a].tracks[t].nextShow = event;
                                        playlistEvents.push(event.artists[a].tracks[t]);
                                    }
                                }
                            }
                        }
                    }
                }
                data.forEach(getEventsArtsts);
                if (playlistEvents.length == 0 && offset < 100) {
                    offset = offset + 20;
                    eventsPlaylist ()
                    console.log(offset)
                }
            })
    }
    function pushTrack (track, art) {
        $scope.newTrack = {};
        if (track.platform == 'Soundcloud') {
            $scope.newTrack.redirectUrl = track.redirectUrl;
        }
        if (track.nextShow != undefined) {
            $scope.newTrack.nextShow = track.nextShow;
        }
        $scope.newTrack.platform = track.platform;
        $scope.newTrack.thumbnailUrl = track.thumbnailUrl;
        $scope.newTrack.url = track.url;
        $scope.newTrack.artist = art;
        $scope.newTrack.title = track.title;
        $scope.newTrack.trackId = track.trackId;
        $rootScope.playlist.tracks.push($scope.newTrack);
    }
    $rootScope.loadPlaylist = function (playlist) {
        $rootScope.playlist.name = playlist.name;
        /*$rootScope.playlist.tracks = [];
        $rootScope.playlist.genres = [];*/
        alert('playlist')
    };
    $rootScope.addToPlaylist = function (tracks, artist) {
        offset = 0;
        if ($rootScope.playlist.tracks.length == 0) {
            var tracksLenght = tracks.length;
            for (var tr = 0; tr < tracksLenght; tr++) {
                pushTrack(tracks[tr], artist)
            }
            $scope.play(i);
            played = [];
        } else if ( $scope.playlistEnd == true) {
            var last = $rootScope.playlist.tracks.length;
            var tracksLenght = tracks.length;
            for (var tr = 0; tr < tracksLenght; tr++) {
                pushTrack(tracks[tr], artist)
            }
            $scope.play(last);
            $scope.playlistEnd = false;
        } else {
            var tracksLenght = tracks.length;
            for (var tr = 0; tr < tracksLenght; tr++) {
                pushTrack(tracks[tr], artist)
            }
        }
        function addGenres (genre) {
            $rootScope.playlist.genres = $rootScope.playlist.genres.concat(genre.name);
        }
        artist.genres.forEach(addGenres);
        eventsPlaylist();
    };
    $rootScope.addAndPlay = function (tracks, artist) {
        offset = 0;
        var last = $rootScope.playlist.tracks.length;
        var tracksLenght = tracks.length;
        for (var tr = 0; tr < tracksLenght; tr++) {
            pushTrack(tracks[tr], artist)
        }
        //Angularytics.trackEvent("listen music", artist);
        $scope.play(last);
        $scope.playlistEnd = false;
        if ($rootScope.playlist.tracks.length == 0) {
            played = [];
        }
        function addGenres (genre) {
            $rootScope.playlist.genres = $rootScope.playlist.genres.concat(genre.name);
        }
        artist.genres.forEach(addGenres);
        eventsPlaylist();
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
                var lastT = $rootScope.playlist.tracks.length;
                if (playlistEvents.length > 0) {
                    for (var t = 0; t < playlistEvents.length; t++) {
                        pushTrack(playlistEvents[t], playlistEvents[t].art)
                    }
                    $rootScope.playlist.name = 'La selection';
                    $rootScope.playlist.by = 'Claude';
                    console.log($rootScope.playlist)
                    $scope.play(lastT)
                    playlistEvents = [];
                } else {
                    $scope.playlistEnd = true;
                }
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
        if ( $rootScope.playlist.tracks[i].nextShow != undefined) {
            $timeout(function () {
                $scope.$apply(function () {
                    $scope.showInfo = true;
                    $timeout(function () {
                        $scope.showInfo = false;
                    }, 5000)
                })
            }, 0)
        }
        $scope.prevTrack = function () {
            i--;
            $scope.play(i);
        };
        $scope.nextTrack = function () {
            $scope.onPlay = false;
            if ($scope.shuffle == true) {
                i = (Math.floor((Math.random() * $rootScope.playlist.tracks.length) + 1));
                shuffle()
            } else {
                if (i < $rootScope.playlist.tracks.length - 1) {
                    i++;
                    $scope.play(i);
                } else {
                    console.log('playlist end')
                    var lastT = $rootScope.playlist.tracks.length;
                    if (playlistEvents.length > 0) {
                        for (var t = 0; t < playlistEvents.length; t++) {
                            pushTrack(playlistEvents[t], playlistEvents[t].art)
                        }
                        $rootScope.playlist.name = 'La selection';
                        $rootScope.playlist.by = 'Claude';
                        console.log($rootScope.playlist)
                        $scope.play(lastT)
                        playlistEvents = [];
                    } else {
                        $scope.playlistEnd = true;
                    }
                }
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
            if (document.getElementById('musicPlayer').currentTime == document.getElementById('musicPlayer').duration) {
                $scope.nextTrack()
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
                document.getElementById('musicPlayer').outerHTML = '<video class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></video>';
                /*document.getElementById('musicPlayer').removeEventListener('ended', $scope.next);
                document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);*/
            }
        };
        $scope.remPlaylist = function () {
            $rootScope.playlist.tracks = [];
            played = [];
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            document.getElementById('musicPlayer').outerHTML = '<video class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></video>';
            document.getElementById('musicPlayer').removeEventListener('ended', $scope.next);
            document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
        };
        document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
        document.getElementById('musicPlayer').addEventListener('error', $scope.nextTrack);
        console.log(document.getElementById('musicPlayer'))
        $scope.trackActive = $rootScope.playlist.tracks[i];
        if ($rootScope.playlist.tracks[i].platform == 'Soundcloud' || $rootScope.window == 'small' || $rootScope.window == 'medium') {
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            /*document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';*/
            if ($rootScope.playlist.tracks[i].url != undefined) {
                if ($rootScope.playlist.tracks[i].platform == 'Soundcloud') {
                    document.getElementById('musicPlayer').setAttribute('src', $rootScope.playlist.tracks[i].url + '?client_id=f297807e1780623645f8f858637d4abb');
                } else {
                    var youtubeId = $rootScope.playlist.tracks[i].url;
                    YoutubeVideo(youtubeId, function (video) {
                        var webm = video.getSource("video/webm", "medium");
                        var mp4 = video.getSource("video/mp4", "medium");
                        document.getElementById('musicPlayer').setAttribute('src', mp4.url);
                        document.getElementById('musicPlayer').play();
                        document.getElementById('musicPlayer').addEventListener('error', $scope.nextTrack);
                    });
                }
            } else {
                $scope.nextTrack();
            }
            document.getElementById('musicPlayer').addEventListener("contextmenu", function (e) { e.preventDefault(); e.stopPropagation(); }, false);

            // hide the controls if they're visible
            if (document.getElementById('musicPlayer').hasAttribute("controls")) {
                document.getElementById('musicPlayer').removeAttribute("controls")
            }
            $timeout(function () {
                $scope.$apply(function () {
                    $scope.onPlay = true;
                });
            },0);
            $scope.playPause = function () {
                if ($scope.onPlay == false) {
                    document.getElementById('musicPlayer').play();
                    $timeout(function(){
                        $scope.$apply(function () {
                            $scope.onPlay = true;
                        });
                    })
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
            document.getElementById('musicPlayer').play();
        } else if ($rootScope.playlist.tracks[i].platform == 'Youtube') {
            document.getElementById('musicPlayer').pause();
            //document.getElementById('musicPlayer').classList.add('ng-hide');
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            //document.getElementById('youtubePlayer').classList.remove('ng-hide');
            document.getElementById('youtubePlayer').setAttribute('src', $rootScope.playlist.tracks[i].url);
            function onPlayerReady(event) {
                console.log('youtube')
                $scope.playPause = function () {
                    if ($scope.onPlay == false) {
                        event.target.playVideo();
                        $scope.onPlay = true;
                    } else {
                        event.target.pauseVideo();
                        $scope.onPlay = false;
                    }
                };
                var yPlayer = event.target;
                yPlayer.setVolume($scope.levelVol);
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
                console.log(window)
                yPlayer.unMute();
                yPlayer.playVideo();
                $scope.onPlay = true;
                $scope.$apply();

                if (i > 0) {
                    function goToTrackActive() {
                        if (document.getElementsByClassName('trackContener').length >= i) {
                            $timeout(function () {
                                var posTrackActive = document.getElementsByClassName('trackContener')[i].getBoundingClientRect();
                                document.getElementsByClassName('playlistScroller')[0].scrollLeft = document.getElementsByClassName('playlistScroller')[0].scrollLeft + posTrackActive.left - 5;
                            }, 100);
                        } else {
                            goToTrackActive()
                        }
                    }

                    goToTrackActive()
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
                if (event.data === 0) {
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
                    'onError': function () {
                        $scope.nextTrack()
                    }
                }
            });
        }
    };
    /*$scope.savePlaylist = function () {
        $http.post('/playlist',
        )
    }*/
}]);