angular.module('claudeApp').
    controller('PlayerCtrl', ['$scope', '$rootScope', '$timeout', '$filter', 'EventsFactory',
        '$modal', 'TracksRecommender', 'UserFactory', 'ArtistsFactory', '$localStorage', 'PlaylistService',
        function ($scope, $rootScope, $timeout, $filter, EventsFactory, $modal, TracksRecommender,
                  UserFactory, ArtistsFactory, $localStorage, PlaylistService) {
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
            //$scope.limitedTracks = [];
            var offset = 0;
            var playlistEvents = [];
            var played = [];
            var i = 0;
            var updateProgressYt;
            var player;
            var stopPush = false;
            if ($localStorage.tracksSignaled === undefined) {
                $localStorage.tracksSignaled = [];
            }
            function calculeNumberToDisplay () {
                if ($rootScope.window === 'large') {
                    $scope.numberToDisplay = 4;
                } else if ($rootScope.window === 'medium') {
                    $scope.numberToDisplay = 3;
                } else if ($rootScope.window === 'small') {
                    $scope.numberToDisplay = 1;
                }
            }
            calculeNumberToDisplay();
            $rootScope.$watch('window', calculeNumberToDisplay);
            $scope.sortableOptions = {
                containment: '#sortable-container',
                //restrict move across columns. move only within column.
                dragEnd : function (a) {
                    var index = $rootScope.playlist.tracks.indexOf(a.source.itemScope.track);
                    var removedElement = $rootScope.playlist.tracks.splice(index, 1)[0];
                    $rootScope.playlist.tracks.splice(a.dest.index + $scope.indexToStart, 0, removedElement);
                },
                accept: function (sourceItemHandleScope, destSortableScope) {
                    return sourceItemHandleScope.itemScope.sortableScope.$id === destSortableScope.$id;
                }
            };
            $scope.changeIndexToStart = function (newVal) {
                $scope.indexToStart = newVal;
                $scope.newIndexToStart = newVal;
                $scope.limitedTracks = $filter('slice')($rootScope.playlist.tracks, $scope.indexToStart, $scope.indexToStart+ $scope.numberToDisplay);
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

            function eventsPlaylist() {
                $rootScope.playlist.genres.map(function(genre) {
                    PlaylistService.getEventsGenrePlaylist(genre).then(function (completePlaylist) {
                        completePlaylist.tracks.map(function(track) {
                            track.nextShow = {};
                            track.nextShow.id = track.nextEventId;
                            playlistEvents.push(track);
                        })
                    })
                });
            }

            function addGenres (genre) {
                $rootScope.playlist.genres = $rootScope.playlist.genres.concat(genre.name);
            }

            function getNextShow (track) {
                ArtistsFactory.getArtistEvents(track.artist.facebookUrl).then(function (events) {
                    events = $filter('orderBy')(events, 'startTime', false);
                    track.nextShow = events[0];
                });
            }

            function pushTrack (track) {
                track.genres.forEach(addGenres);
                $scope.newTrack = {};
                if ($rootScope.favoritesTracks) {
                    if ($rootScope.favoritesTracks.indexOf(track.uuid) > -1) {
                        track.isFavorite = true;
                        $scope.newTrack.isFavorite = true;
                    }
                }
                if (track.platform === 's') {
                    $scope.newTrack.redirectUrl = track.redirectUrl;
                }
                if (track.nextShow !== undefined) {
                    $scope.newTrack.nextShow = track.nextShow;
                }
                $scope.newTrack.platform = track.platform;
                $scope.newTrack.thumbnailUrl = track.thumbnailUrl;
                $scope.newTrack.url = track.url;
                $scope.newTrack.artist = {name: track.artistName,
                    facebookUrl: track.artistFacebookUrl};
                $scope.newTrack.title = track.title;
                $scope.newTrack.uuid = track.uuid;
                $rootScope.playlist.tracks.push($scope.newTrack);
                getNextShow($rootScope.playlist.tracks[$rootScope.playlist.tracks.length-1]);
                $scope.limitedTracks = $filter('slice')($rootScope.playlist.tracks, $scope.indexToStart, $scope.indexToStart+ $scope.numberToDisplay)
            }

            function pushListOfTracks (tracks, play) {
                var tracksLenght = tracks.length;
                pushTrack(tracks[0]);
                if (play === true) {
                    $scope.play($rootScope.playlist.tracks.length-1);
                }
                if (tracksLenght < 10) {
                    for (var tr = 1; tr < tracksLenght; tr++) {
                        pushTrack(tracks[tr])
                    }
                } else {
                    var start = 1;
                    var end = 10;
                    function addLotOfTracks (start, end) {
                        for (var tr = start; tr < end; tr++) {
                            if (stopPush === true) {
                                return;
                            }
                            if (tracks[tr] !== undefined) {
                                pushTrack(tracks[tr])
                            }
                        }
                        if (end < tracksLenght && stopPush === false) {
                            $timeout(function () {
                                addLotOfTracks(end, end + 10)
                            },10)
                        }
                    }
                    addLotOfTracks(start, end);

                }
            }

            $rootScope.addToPlaylist = function (tracks) {
                stopPush = false;
                offset = 0;
                if ($rootScope.playlist.tracks.length === 0) {
                    pushListOfTracks(tracks, true);
                    played = [];
                    $scope.playlistEnd = false;
                } else if ( $scope.playlistEnd === true) {
                    pushListOfTracks(tracks, true);
                    $scope.playlistEnd = false;
                } else {
                    pushListOfTracks(tracks, false);
                    $scope.playlistEnd = false;
                }
                tracks.map(function(track) {
                    track.genres.forEach(addGenres);
                    return track
                });

                eventsPlaylist();
            };

            $rootScope.loadPlaylist = function (playlist) {
                stopPush = false;
                $rootScope.playlist.name = playlist.name;
                $rootScope.playlist.id = playlist.id;
                $rootScope.playlist.tracks = [];
                $rootScope.playlist.genres = [];
                pushListOfTracks(playlist.tracks, true);
                $scope.playlistEnd = false;
                played = [];
            };

            $rootScope.addAndPlay = function (tracks) {
                stopPush = false;
                offset = 0;
                pushListOfTracks(tracks, true);
                tracks.map(function(track) {
                    track.genres.forEach(addGenres);
                });
                eventsPlaylist();
            };

            // Player functions //

            function alreadyPlayed (element) {
                return played.indexOf(element.title) > -1;
            }

            function shuffle () {
                if ( i > $rootScope.playlist.tracks.length - 1) {
                    i = (Math.floor((Math.random() * $rootScope.playlist.tracks.length) + 1));
                    shuffle()
                } else if (played.indexOf($rootScope.playlist.tracks[i].title) > -1) {
                    if ($rootScope.playlist.tracks.every(alreadyPlayed) === true) {
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
                if ($rootScope.playlist.tracks[i].platform === 's' || $rootScope.window === 'small'
                    || $rootScope.window === 'medium') {
                    TracksRecommender.trackRateByTime(document.getElementById('musicPlayer').duration,
                        document.getElementById('musicPlayer').currentTime,
                        $rootScope.playlist.tracks[i].id);
                } else if (player !== undefined) {
                    TracksRecommender.trackRateByTime(player.getDuration(),
                    player.getCurrentTime(),
                        $rootScope.playlist.tracks[i].id);
                }
                if ($scope.shuffle === true) {
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
                $scope.limitedTracks.splice(index, 1);
                $rootScope.playlist.tracks.splice(index + $scope.indexToStart, 1);
                $scope.limitedTracks = $filter('slice')($rootScope.playlist.tracks, $scope.indexToStart, $scope.indexToStart+ $scope.numberToDisplay);
                if (index + $scope.indexToStart === $rootScope.playlist.tracks.length &&
                    index + $scope.indexToStart === i) {
                    $rootScope.playlist.tracks = [];
                    played = [];
                    playlistEvents = [];
                    document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                    document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
                    document.getElementById('minVideoPlayer').outerHTML = '<video id="minVideoPlayer" class="marginAuto width100p" controls="controls" style="height: 200px"></video>';
                    $scope.showVideo = false;
                }
            };

            $scope.remPlaylist = function () {
                stopPush = true;
                $rootScope.playlist.tracks = [];
                $rootScope.playlist.genres = [];
                $rootScope.playlist.id = '';
                $rootScope.playlist.name = '';
                played = [];
                playlistEvents = [];
                document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                document.getElementById('musicPlayer').outerHTML = '<audio class="width100p ng-hide" id="musicPlayer" style="position: fixed" autoplay></audio>';
                document.getElementById('minVideoPlayer').outerHTML = '<video id="minVideoPlayer" class="marginAuto width100p" controls="controls" style="height: 200px"></video>';
                $scope.showVideo = false;
                document.getElementById('musicPlayer').removeEventListener('ended', $scope.nextTrack);
                document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
                document.getElementById('musicPlayer').removeEventListener("error", error);
            };

            function error () {
                $scope.error = 'Désolé une erreur de lecture s\'est produite.';
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
                $scope.changeIndexToStart(trackIndex);
                //display nextShow info//
                if ( $rootScope.playlist.tracks[i].nextShow !== undefined) {
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
                clearInterval(updateProgressYt);
                document.getElementById('musicPlayer').removeEventListener("timeupdate", updateProgress);
                document.getElementById('musicPlayer').removeEventListener('error', error);

                $scope.trackActive = $rootScope.playlist.tracks[i];

                if ($rootScope.playlist.tracks[i].platform === 's' || $rootScope.window === 'small' || $rootScope.window === 'medium') {
                    document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                    if ($rootScope.playlist.tracks[i].url !== undefined) {
                        if ($rootScope.playlist.tracks[i].platform === 's') {
                            document.getElementById('musicPlayer').setAttribute('src', $rootScope.playlist.tracks[i].url + '?client_id=f297807e1780623645f8f858637d4abb');
                        } else {
                            var youtubeId = $rootScope.playlist.tracks[i].url;
                            YoutubeVideo(youtubeId, function (video) {
                                if (video.status === 'ok') {
                                    var webm = video.getSource("video/webm", "medium");
                                    var mp4 = video.getSource("video/mp4", "medium");
                                    document.getElementById('musicPlayer').setAttribute('src', mp4.url);
                                    document.getElementById('musicPlayer').play();
                                    $scope.$apply(function () {
                                        $scope.onPlay = true;
                                    });
                                    $scope.insertVideo = function () {
                                        if ($scope.showVideo === true) {
                                            document.getElementById('minVideoPlayer').setAttribute('src', mp4.url);
                                            document.getElementById('minVideoPlayer').play();
                                            document.getElementById('minVideoPlayer').currentTime = document.getElementById('musicPlayer').currentTime;
                                            document.getElementById('musicPlayer').pause();
                                            $scope.playPause = function () {
                                                if ($scope.onPlay === false) {
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
                                            document.getElementById('minVideoPlayer').outerHTML = '<video id="minVideoPlayer" class="marginAuto width100p" controls="controls" style="height: 200px"></video>';
                                            document.getElementById('musicPlayer').play();
                                            $scope.playPause = function () {
                                                if ($scope.onPlay === false) {
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
                                    error();
                                }
                            });
                        }
                    } else {
                        $scope.nextTrack();
                    }

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
                        if ($scope.onPlay === false) {
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
                    document.getElementById('musicPlayer').addEventListener('error', error);
                    document.getElementById('musicPlayer').addEventListener("timeupdate", updateProgress);
                    document.getElementById('musicPlayer').play();
                } else if ($rootScope.playlist.tracks[i].platform === 'y') {
                    document.getElementById('musicPlayer').pause();
                    document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
                    document.getElementById('youtubePlayer').setAttribute('src', $rootScope.playlist.tracks[i].url);
                    clearInterval(updateProgressYt);
                    function onPlayerReady(event) {
                        clearInterval(updateProgressYt);
                        $scope.playPause = function () {
                            if ($scope.onPlay === false) {
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
                    }

                    function onPlayerStateChange(event) {
                        if (event.data === 1) {
                            clearInterval(updateProgressYt);
                            var duration = player.getDuration();
                            var curentDuration = player.getCurrentTime();
                            updateProgressYt = setInterval(function () {
                                curentDuration = player.getCurrentTime();
                                var prog = document.getElementById("progress");
                                var val = 0;
                                if (curentDuration > 0) {
                                    val = 100 / duration * curentDuration;
                                }
                                prog.style.width = val + "%";
                                document.getElementById('currentTime').innerHTML =
                                    readableDuration(curentDuration) +
                                    ' / ' + readableDuration(duration);
                                if (val === 100) {
                                    clearInterval(updateProgressYt);
                                }
                            }, 100);
                        } else {
                            clearInterval(updateProgressYt);
                        }
                        if (event.data === 0) {
                            clearInterval(updateProgressYt);
                            $scope.nextTrack()
                        }
                    }
                    player = new YT.Player('youtubePlayer', {
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