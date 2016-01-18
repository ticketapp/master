angular.module('claudeApp').controller('EventMinCtrl', ['$scope', 'UserFactory', 'InfoModal', 'TrackService', '$rootScope',
    function ($scope, UserFactory, InfoModal, TrackService, $rootScope) {

        $scope.isFollowedPlace = function (place) {
            UserFactory.getIsFollowedPlace(place.id).then(function (isFollowed) {
                if (isFollowed === true || isFollowed === false) {
                    place.isFollowed = isFollowed;
                }
            })
        };

        $scope.followPlace = function (place) {
            UserFactory.followPlaceByPlaceId(place.id, place.name).then(function (follow) {
                if (follow !== 'error') {
                    place.isFollowed = true;
                    InfoModal.displayInfo('Vous suivez ' + place.name)
                }
            })
        };

        $scope.unfollowPlace = function (place) {
            UserFactory.unfollowPlace(place.id, place.name).then(function (follow) {
                if (follow !== 'error') {
                    place.isFollowed = false;
                    InfoModal.displayInfo('Vous ne suivez plus ' + place.name)
                }
            })
        };

        $scope.isFollowedArtist = function (artist) {
            UserFactory.ArtistIsFollowed(artist.id).then(function (isFollowed) {
                if (isFollowed === true || isFollowed === false) {
                    artist.isFollowed = isFollowed;
                }
            })
        };

        $scope.followArtist = function (artist) {
            UserFactory.followArtistByArtistId(artist.id, artist.name).then(function (follow) {
                if (follow !== 'error') {
                    artist.isFollowed = true;
                    InfoModal.displayInfo('Vous suivez ' + artist.name)
                }
            })
        };

        $scope.unfollowArtist = function (artist) {
            UserFactory.unfollowArtist(artist.id, artist.name).then(function (follow) {
                if (follow !== 'error') {
                    artist.isFollowed = false;
                    InfoModal.displayInfo('Vous ne suivez plus ' + artist.name)
                }
            })
        };

        $scope.isFollowedOrganizer = function (organizer) {
            UserFactory.getIsFollowedOrganizer(organizer.id).then(function (isFollowed) {
                if (isFollowed === true || isFollowed === false) {
                    organizer.isFollowed = isFollowed;
                }
            })
        };

        $scope.followOrganizer = function (organizer) {
            UserFactory.followOrganizerByOrganizerId(organizer.id, organizer.name).then(function (follow) {
                if (follow !== 'error') {
                    organizer.isFollowed = true;
                    InfoModal.displayInfo('Vous suivez ' + organizer.name)
                }
            })
        };

        $scope.unfollowOrganizer = function (organizer) {
            UserFactory.unfollowOrganizer(organizer.id, organizer.name).then(function (follow) {
                if (follow !== 'error') {
                    organizer.isFollowed = false;
                    InfoModal.displayInfo('Vous ne suivez plus ' + organizer.name)
                }
            })
        };

        $scope.getTracksAndPlay = function(artist) {
            TrackService.getArtistTracks(artist.facebookUrl).then(function(tracks) {
                tracks.map(function(track) {
                    track.genres = artist.genres;
                    return track;
                });
                $rootScope.addAndPlay(tracks, artist)
            })
        }
    }]);