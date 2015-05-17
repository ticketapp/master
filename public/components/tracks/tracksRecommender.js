angular.module('claudeApp').factory('TracksRecommender', ['$http', '$rootScope',
    function ($http, $rootScope) {
    var factory = {
        postTrackRate: function (trackId, rate) {
            $http.post('/tracks/'+ trackId + '/' + rate + '/rating')
        },
        trackRateByTime : function (duration, time, trackId) {
            var rate;
            console.log(time);
            if ($rootScope.connected == true) {
                if (time <= 10) {
                    rate = '-' + Math.round(time*10);
                } else if (100 / duration * time < 95) {
                    var t = 100 / duration * time;
                    rate = '-' + Math.round((100 - t) - (t ^ 1.08 / 100 * (100 - t)));
                } else {
                    rate = 100;
                }
                factory.postTrackRate(trackId, rate)
            }
        },
        UpsertTrackRate : function (isPositive, trackId) {
            if ($rootScope.connected == true) {
                var rate = 1000;
                if (isPositive == false) {
                    rate = -1000
                }
                factory.postTrackRate(trackId, rate)
            }
        }
    };
    return factory;
}]);