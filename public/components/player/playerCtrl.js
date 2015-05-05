angular.module('claudeApp').
    controller('PlayerCtrl', ['$scope', '$rootScope', '$timeout', '$filter', 'EventsFactory', '$modal',
        function ($scope, $rootScope, $timeout, $filter, EventsFactory, $modal) {
            $rootScope.playlist = {
                name : '',
                genres: [],
                by: '',
                tracks: []
            };
            $scope.showLecteur = true;
            $scope.playPause = "";
            $scope.onPlay = false;
            $scope.levelVol = 100;
            $scope.shuffle = false;
            $scope.playlistEnd = false;
            $scope.error = '';
            var offset = 0;
            var playlistEvents = [];
            var played = [];
            var artNames = [];
            var i = 0;
            $scope.sortableOptions = {
                containment: '#sortable-container',
                //restrict move across columns. move only within column.
                accept: function (sourceItemHandleScope, destSortableScope) {
                    return sourceItemHandleScope.itemScope.sortableScope.$id === destSortableScope.$id;
                }
            };

            /* modify playlist */

            $scope.savePlaylist = function () {
                var modalInstance = $modal.open({
                    templateUrl: 'assets/components/player/savePlaylistForm.html',
                    controller: 'savePlaylistCtrl'
                });
                modalInstance.result.then( function () {
                });
            };

            function eventsPlaylist () {
                function getEventsByGenre (genre) {
                    EventsFactory.getEventsByGenre(genre, offset, $rootScope.geoLoc).then(
                    function (events) {
                        function getEventsArtists(event) {
                            var evArtLenght = event.artists.length;
                            for (var a = 0; a < evArtLenght; a++) {
                                if (artNames.toString().indexOf(event.artists[a].name) == -1) {
                                    artNames.push(event.artists[a].name);
                                    for (var t = 0; t < 4; t++) {
                                        if (event.artists[a].tracks[t] != undefined) {
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
                        events.forEach(getEventsArtists);
                    });
                }
                $rootScope.playlist.genres.forEach(getEventsByGenre)
            }

            function pushTrack (track, art) {
                $scope.newTrack = {};
                if (track.platform == 's') {
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

            function pushListOfTracks (artist, tracks, play) {
                var tracksLenght = tracks.length;
                var needRepeat = false;
                if (tracksLenght > 10) {
                    tracksLenght = 10;
                    needRepeat = true
                }
                for (var tr = 0; tr < tracksLenght; tr++) {
                    pushTrack(tracks[tr], artist)
                }
                if (play == true) {
                    $scope.play($rootScope.playlist.tracks.length);
                }
                if (needRepeat == true) {
                    tracksLenght = tracks.length;
                    var start = 10;
                    function pushAllTracks (start) {
                        for (tr = start; tr < start + 100; tr++) {
                            if (tracks[tr] != undefined) {
                                pushTrack(tracks[tr], artist)
                            } else {
                                return;
                            }
                        }
                    }
                    if (start < tracksLenght) {
                        start = start + 100;
                        pushAllTracks(start)
                    }
                }
            }

            function addGenres (genre) {
                $rootScope.playlist.genres = $rootScope.playlist.genres.concat(genre.name);
            }

            $rootScope.addToPlaylist = function (tracks, artist) {
                offset = 0;
                $scope.playlistEnd = false;
                if ($rootScope.playlist.tracks.length == 0) {
                    pushListOfTracks(artist, tracks, true);
                    played = [];
                } else if ( $scope.playlistEnd == true) {
                    pushListOfTracks(artist, tracks, true);
                } else {
                    pushListOfTracks(artist, tracks, false);
                }
                artist.genres.forEach(addGenres);
                eventsPlaylist();
            };

            $rootScope.loadPlaylist = function (playlist) {
                $rootScope.playlist.name = playlist.name;
                $rootScope.playlist.playlistId = playlist.playlistId;
                $rootScope.playlist.tracks = [];
                $rootScope.playlist.genres = [];
                var tracksLenght = playlist.tracks.length;
                var tr = 0;
                playlist.tracks = $filter('orderBy')(playlist.tracks, 'rank', false);
                function addtr (tr) {
                    $http.get('/artists/' + playlist.tracks[tr].artistFacebookUrl).
                        success(function (data) {
                            pushTrack(playlist.tracks[tr], data);
                            if (tr == 0) {
                                $scope.play(i);
                            }
                            if (tr < tracksLenght -1) {
                                addtr(tr + 1)
                            }
                            function addGenres (genre) {
                                $rootScope.playlist.genres = $rootScope.playlist.genres.concat(genre.name);
                            }
                            data.genres.forEach(addGenres);
                            eventsPlaylist();
                        })
                }
                addtr(tr);
                $scope.playlistEnd = false;
                played = [];
            };

            $rootScope.addAndPlay = function (tracks, artist) {
                offset = 0;
                pushListOfTracks(artist, tracks, true);
                artist.genres.forEach(addGenres);
                eventsPlaylist();
            };

            // Player functions //

            function alreadyPlayed (element, index, array) {
                return played.indexOf(element.title) > -1;
            }

            function shuffle () {
                if ( i > $rootScope.playlist.tracks.length - 1) {
                    i = (Math.floor((Math.random() * $rootScope.playlist.tracks.length) + 1));
                    shuffle()
                } else if (played.indexOf($rootScope.playlist.tracks[i].title) > -1) {
                    if ($rootScope.playlist.tracks.every(alreadyPlayed) == true) {
                        var lastT = $rootScope.playlist.tracks.length;
                        if (playlistEvents.length > 0) {
                            for (var t = 0; t < playlistEvents.length; t++) {
                                pushTrack(playlistEvents[t], playlistEvents[t].art)
                            }
                            $scope.play(lastT);
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
                        var lastT = $rootScope.playlist.tracks.length;
                        if (playlistEvents.length > 0) {
                            for (var t = 0; t < playlistEvents.length; t++) {
                                pushTrack(playlistEvents[t], playlistEvents[t].art)
                            }
                            $scope.play(lastT);
                            playlistEvents = [];
                        } else {
                            $scope.playlistEnd = true;
                        }
                    }
                }
            };

            $scope.closeTrack = function (index) {
                $rootScope.playlist.tracks.splice(index, 1);
                if (index == $rootScope.playlist.tracks.length && index == i) {
                    $rootScope.playlist.tracks = [];
                    played = [];
                    playlistEvents = [];
                    document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                    document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
                    document.getElementById('minVideoPlayer').outerHTML = '<video id="minVideoPlayer" class="marginAuto width100p" controls="controls" style="height: 200px"></video>'
                    $scope.showVideo = false;
                }
            };

            $scope.remPlaylist = function () {
                $rootScope.playlist.tracks = [];
                played = [];
                playlistEvents = [];
                document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
                document.getElementById('minVideoPlayer').outerHTML = '<video id="minVideoPlayer" class="marginAuto width100p" controls="controls" style="height: 200px"></video>'
                $scope.showVideo = false;
                document.getElementById('musicPlayer').removeEventListener('ended', $scope.nextTrack);
                document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
                document.getElementById('musicPlayer').removeEventListener("error", error);
            };

            function error () {
                $scope.error = 'Désolé une erreur de lecture s\'est produite.'
                $timeout(function () {
                    $scope.error = ''
                }, 2000);
                $scope.nextTrack();
            }

            function updateProgress() {
                var progress = document.getElementById("progress");
                var value = 0;
                if (document.getElementById('musicPlayer').currentTime > 0) {
                    value = 100 / document.getElementById('musicPlayer').duration *
                        document.getElementById('musicPlayer').currentTime;
                }
                if (document.getElementById('musicPlayer').currentTime ==
                    document.getElementById('musicPlayer').duration) {
                        $scope.nextTrack()
                }
                progress.style.width = value + "%";
                document.getElementById('currentTime').innerHTML =
                    readableDuration(document.getElementById('musicPlayer').currentTime) +
                    ' / ' + readableDuration(document.getElementById('musicPlayer').duration);
            }

            $scope.play = function (trackIndex) {
                i = trackIndex;
                //display nextShow info//
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

                //pass track to played//
                played.push($rootScope.playlist.tracks[i].title);

                //clear yt progressBar interval and listeners//
                if (typeof(updateProgressYt) != "undefined") {
                    clearInterval(updateProgressYt);
                }
                document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
                document.getElementById('musicPlayer').removeEventListener('error', error);

                $scope.trackActive = $rootScope.playlist.tracks[i];

                if ($rootScope.playlist.tracks[i].platform == 's' || $rootScope.window == 'small' || $rootScope.window == 'medium') {
                    document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                    if ($rootScope.playlist.tracks[i].url != undefined) {
                        if ($rootScope.playlist.tracks[i].platform == 's') {
                            document.getElementById('musicPlayer').setAttribute('src', $rootScope.playlist.tracks[i].url + '?client_id=f297807e1780623645f8f858637d4abb');
                        } else {
                            var youtubeId = $rootScope.playlist.tracks[i].url;
                            YoutubeVideo(youtubeId, function (video) {
                                if (video.status == 'ok') {
                                    var webm = video.getSource("video/webm", "medium");
                                    var mp4 = video.getSource("video/mp4", "medium");
                                    document.getElementById('musicPlayer').setAttribute('src', mp4.url);
                                    document.getElementById('musicPlayer').play();
                                    $scope.$apply(function () {
                                        $scope.onPlay = true;
                                    });
                                    document.getElementById('musicPlayer').addEventListener('error', error);
                                    $scope.insertVideo = function () {
                                        if ($scope.showVideo == true) {
                                            document.getElementById('minVideoPlayer').setAttribute('src', mp4.url);
                                            document.getElementById('minVideoPlayer').play();
                                            document.getElementById('minVideoPlayer').currentTime = document.getElementById('musicPlayer').currentTime;
                                            document.getElementById('musicPlayer').pause();
                                            $scope.playPause = function () {
                                                if ($scope.onPlay == false) {
                                                    document.getElementById('minVideoPlayer').play();
                                                    $timeout(function(){
                                                        $scope.$apply(function () {
                                                            $scope.onPlay = true;
                                                        });
                                                    })
                                                } else {
                                                    document.getElementById('minVideoPlayer').pause();
                                                    $scope.onPlay = false;
                                                }
                                            };
                                        } else {
                                            document.getElementById('musicPlayer').currentTime = document.getElementById('minVideoPlayer').currentTime;
                                            document.getElementById('minVideoPlayer').outerHTML = '<video id="minVideoPlayer" class="marginAuto width100p" controls="controls" style="height: 200px"></video>'
                                            document.getElementById('musicPlayer').play();
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
                                        }
                                    }
                                } else {
                                    document.getElementById('musicPlayer').addEventListener('error', error);
                                }
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
                                    if (document.getElementsByClassName('trackContener')[i] != undefined) {
                                        var posTrackActive = document.getElementsByClassName('trackContener')[i].getBoundingClientRect();
                                        document.getElementsByClassName('playlistScroller')[0].scrollLeft = document.getElementsByClassName('playlistScroller')[0].scrollLeft + posTrackActive.left - 5;
                                    }
                                }, 100);
                            } else {
                                goToTrackActive ()
                            }
                        }
                        goToTrackActive ()
                    }
                    document.getElementById('musicPlayer').play();
                } else if ($rootScope.playlist.tracks[i].platform == 'y') {
                    document.getElementById('musicPlayer').pause();
                    document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                    document.getElementById('youtubePlayer').setAttribute('src', $rootScope.playlist.tracks[i].url);
                    function onPlayerReady(event) {
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
                                error()
                            }
                        }
                    });
                }
            };

    }]);