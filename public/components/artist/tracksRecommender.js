angular.module('claudeApp').factory('TracksRecommender', ['$http', function ($http) {
    var factory = {
        postTrackRate: function (trackId, rate) {
            $http.post('/tracks/'+ trackId + '/' + rate + '/rating')
        },
        trackRateByTime : function (duration, time, trackId) {
            var rate;
            if (time <= 10000) {
                rate = '-' + Math.round(time/100);
            } else if (100/duration*time < 95) {
                var t = 100/duration*time;
                rate = '-' + Math.round((100-t)-(t^1.08/100*(100-t)));
            } else {
                rate = 100;
            }
            factory.postTrackRate(trackId, rate)
        },
        UpsertTrackRate : function (isPositive, trackId) {
            var rate = 1000;
            if (isPositive == false) {
                rate = -1000
            }
            factory.postTrackRate(trackId, rate)
        }
    };
    return factory;
}]);