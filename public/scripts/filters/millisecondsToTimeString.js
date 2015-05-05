angular.module('claudeApp').filter('millSecondsToTimeString', function() {
    return function(millseconds) {
        var seconds = Math.floor(millseconds*3600000 / 1000);
        var days = Math.floor(seconds / 86400);
        var hours = Math.floor((seconds % 86400) / 3600);
        var months = Math.floor(days / 30);
        var timeString = '';

        if(months > 0) timeString += (months > 1) ? (months + " mois ") : (months + " mois ");
        if(days > 0 && months == 0) timeString += (days > 1) ? (days + " jours ") : (days + " jours ");
        if(hours > 0) timeString += (hours > 1) ? (hours + " heures ") : (hours + " heure ");
        return timeString;
    }
});