app.controller ('lecteurCtrl', ['$scope', '$rootScope', function ($scope, $rootScope){
    $rootScope.playlist = [];
    var i = 0;
    $rootScope.addToPlaylist = function (tracks) {
        function pushTrack (el) {
            $rootScope.playlist.push(el);
        }
        tracks.forEach(pushTrack);
        console.log($rootScope.playlist);
        $scope.play(i)
    };
    $scope.play = function (i) {
        $scope.trackActive = $rootScope.playlist[i];
        console.log( $rootScope.playlist[i]);
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
