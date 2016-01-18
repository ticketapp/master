angular.module('claudeApp').
    controller('ArtistFacebookMinCtrl', ['$scope', 'ArtistsFactory', function ($scope, ArtistsFactory) {
        $scope.createArtist = function (artist) {
            ArtistsFactory.createNewArtist(artist);
            window.location.href =('#/artists/' + artist.facebookUrl);
        }
    }]);