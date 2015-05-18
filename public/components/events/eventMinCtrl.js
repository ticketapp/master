angular.module('claudeApp').controller('EventMinCtrl', ['$scope', 'UserFactory',
    function ($scope, UserFactory) {

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
                }
            })
        };

        $scope.unfollowPlace = function (place) {
            UserFactory.unfollowPlace(place.placeId, place.name).then(function (follow) {
                if (follow != 'error') {
                    place.isFollowed = false;
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
                }
            })
        };

        $scope.unfollowArtist = function (artist) {
            UserFactory.unfollowArtist(artist.artistId, artist.name).then(function (follow) {
                if (follow != 'error') {
                    artist.isFollowed = false;
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
            OrganizerFactory.followOrganizerByOrganizerId(organizer.organizerId, organizer.name).then(function (follow) {
                if (follow != 'error') {
                    organizer.isFollowed = true;
                }
            })
        };

        $scope.unfollowOrganizer = function (organizer) {
            UserFactory.unfollowOrganizer(organizer.organizerId, organizer.name).then(function (follow) {
                if (follow != 'error') {
                    organizer.isFollowed = false;
                }
            })
        };
    }]);