app.controller ('lecteurCtrl', ['$scope', '$rootScope', function ($scope, $rootScope){
    $rootScope.playlist = [];
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
    };
    $scope.play = function (i) {
        $scope.trackActive = i;
        if (i > 0) {
            var posTrackActive = document.getElementsByClassName('trackContener')[i].getBoundingClientRect();
            document.getElementsByClassName('ng-lecture')[0].scrollLeft = posTrackActive.left;
        }
        if ($rootScope.playlist[i].from == 'soundcloud') {
            document.getElementById('musicPlayer').setAttribute('src', $rootScope.playlist[i].url + '?client_id=f297807e1780623645f8f858637d4abb');
        }
        document.getElementById('musicPlayer').addEventListener('ended', function () {
            i++;
            $scope.play(i);
        })
    };
    //play(i)
}]);
