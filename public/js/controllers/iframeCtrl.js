app.controller('iframeCtrl', function ($http, $scope, $rootScope){
    var offset =  0;
    $scope.events = [];
    console.log($rootScope.geoLoc);
    function getEvents () {
        $http.get('/events/offset/' + offset + '/' + $rootScope.geoLoc).
            success(function (data, status, headers, config) {
                var scopeIdList = [];

                function getEventId(el, index, array) {
                    scopeIdList.push(el.eventId);
                }
                $scope.events.forEach(getEventId);
                    function uploadEvents(el, index, array) {
                        if (scopeIdList.indexOf(el.eventId) == -1) {
                            el.priceColor = 'rgb(0, 140, 186)';
                            if (el.tariffRange != undefined) {
                                var tariffs = el.tariffRange.split('-');
                                if (tariffs[1] > tariffs[0]) {
                                    el.tariffRange = tariffs[0].replace('.0', '') + '€ - ' +
                                        tariffs[1].replace('.0', '') + '€';
                                } else {
                                    el.tariffRange = tariffs[0].replace('.0', '') + '€';
                                }
                                el.priceColor = 'rgb(' + tariffs[0]*10 + ',' + (250 - (tariffs[0]*10 ) )+
                                    ',' + (175 - (tariffs[0]*10 )) + ')'
                            }
                            $scope.events.push(el);
                        }
                    }

                    data.forEach(uploadEvents)
                console.log($scope.events)
                $rootScope.resizeImgHeight();
            }).
            error(function (data, status, headers, config) {
                // called asynchronously if an error occurs
                // or server returns response with an error status.
            });
    }
    $scope.moreLimit = function () {
        offset = offset + 20;
        $rootScope.resizeImgHeight();
        getEvents();
    };
    getEvents();
    $rootScope.$watch('geoLoc', function (newval) {
        console.log(newval)
        getEvents();
    })
});