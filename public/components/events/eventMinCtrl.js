angular.module('claudeApp').controller('EventMinCtrl', ['$scope', 'UserFactory', 'InfoModal',
    function ($scope, UserFactory, InfoModal) {

        $scope.isFollowedPlace = function (place) {
            UserFactory.getIsFollowedPlace(place.placeId).then(function (isFollowed) {
                if (isFollowed == true || isFollowed == false) {
                    place.isFollowed = isFollowed;
                }
            })
        };

        $scope.followPlace = function (place) {
            UserFactory.followPlaceByPlaceId(place.placeId, place.name).then(function (follow) {
                if (follow != 'error') {
                    place.isFollowed = true;
                    InfoModal.displayInfo('Vous suivez ' + place.name)
                }
            })
        };

        $scope.unfollowPlace = function (place) {
            UserFactory.unfollowPlace(place.placeId, place.name).then(function (follow) {
                if (follow != 'error') {
                    place.isFollowed = false;
                    InfoModal.displayInfo('Vous ne suivez plus ' + place.name)
                }
            })
        };

        $scope.isFollowedArtist = function (artist) {
            UserFactory.ArtistIsFollowed(artist.artistId).then(function (isFollowed) {
                if (isFollowed == true || isFollowed == false) {
                    artist.isFollowed = isFollowed;
                }
            })
        };

        $scope.followArtist = function (artist) {
            UserFactory.followArtistByArtistId(artist.artistId, artist.name).then(function (follow) {
                if (follow != 'error') {
                    artist.isFollowed = true;
                    InfoModal.displayInfo('Vous suivez ' + artist.name)
                }
            })
        };

        $scope.unfollowArtist = function (artist) {
            UserFactory.unfollowArtist(artist.artistId, artist.name).then(function (follow) {
                if (follow != 'error') {
                    artist.isFollowed = false;
                    InfoModal.displayInfo('Vous ne suivez plus ' + artist.name)
                }
            })
        };

        $scope.isFollowedOrganizer = function (organizer) {
            UserFactory.getIsFollowedOrganizer(organizer.organizerId).then(function (isFollowed) {
                if (isFollowed == true || isFollowed == false) {
                    organizer.isFollowed = isFollowed;
                }
            })
        };

        $scope.followOrganizer = function (organizer) {
            UserFactory.followOrganizerByOrganizerId(organizer.organizerId, organizer.name).then(function (follow) {
                if (follow != 'error') {
                    organizer.isFollowed = true;
                    InfoModal.displayInfo('Vous suivez ' + organizer.name)
                }
            })
        };

        $scope.unfollowOrganizer = function (organizer) {
            UserFactory.unfollowOrganizer(organizer.organizerId, organizer.name).then(function (follow) {
                if (follow != 'error') {
                    organizer.isFollowed = false;
                    InfoModal.displayInfo('Vous ne suivez plus ' + organizer.name)
                }
            })
        };
    }]);