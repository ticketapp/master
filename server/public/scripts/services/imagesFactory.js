angular.module('claudeApp').factory('ImagesFactory', function () {
    return function (artist) {
        if (artist.imagePath) {
            artist.oldImagePath = artist.imagePath;
            artist.imagePath = artist.imagePath.split('\\')[0];
        }
        return artist;
    }
});