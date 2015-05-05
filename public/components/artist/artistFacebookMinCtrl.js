angular.module('claudeApp').
    controller('ArtistFacebookMinCtrl', ['$scope', 'ArtistsFactory', function ($scope, ArtistsFactory) {
        $scope.createArtist = function (artist) {
            ArtistsFactory.createNewArtistAndPassItToRootScope(artist);
            window.location.href =('#/artiste/' + artist.facebookUrl);
        }
    }]);