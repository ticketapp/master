app.controller ('lecteurCtrl', ['$scope', '$rootScope', '$timeout', '$http', function ($scope, $rootScope, $timeout, $http){
    $rootScope.playlist = [];
    $scope.showLecteur = true;
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
    $scope.play = function (i) {
        $scope.trackActive = i;
        if ($rootScope.playlist[i].from == 'soundcloud') {
            document.getElementById('youtubePlayer').classList.add('ng-hide');
            document.getElementById('musicPlayer').classList.remove('ng-hide');
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            document.getElementById('musicPlayer').setAttribute('src', $rootScope.playlist[i].url + '?client_id=f297807e1780623645f8f858637d4abb');

        } else if ($rootScope.playlist[i].from == 'youtube') {
            document.getElementById('musicPlayer').pause();
            document.getElementById('musicPlayer').classList.add('ng-hide');
            document.getElementById('youtubePlayer').outerHTML = "<div id='youtubePlayer'></div>";
            document.getElementById('youtubePlayer').classList.remove('ng-hide');
            document.getElementById('youtubePlayer').setAttribute('src', $rootScope.playlist[i].url);
            function onPlayerReady(event) {
                event.target.playVideo();
            }

            // when video ends
            function onPlayerStateChange(event) {
                if(event.data === 0) {
                    i++;
                    $scope.play(i);
                }
            }
            var player = new YT.Player('youtubePlayer', {
                height: '190',
                width: '220',
                videoId: $rootScope.playlist[i].url,
                events: {
                    'onReady': onPlayerReady,
                    'onStateChange': onPlayerStateChange
                }
            });

        }
        if (i > 0) {
            function goToTrackActive () {
                if (document.getElementsByClassName('trackContener').length >= i) {
                    $timeout(function() {
                        var posTrackActive = document.getElementsByClassName('trackContener')[i].getBoundingClientRect();
                        document.getElementsByClassName('playlistScroller')[0].scrollLeft = document.getElementsByClassName('playlistScroller')[0].scrollLeft + posTrackActive.left - 220;
                    }, 10);
                } else {
                    goToTrackActive ()
                }
            }
            goToTrackActive ()
        }
        document.getElementById('musicPlayer').addEventListener('ended', function () {
            i++;
            $scope.play(i);
        });

    };
    //play(i)
}]);
