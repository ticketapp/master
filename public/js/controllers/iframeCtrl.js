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
                if ($scope.events.length == 0) {
                    $scope.events = data;
                } else {
                    function uploadEvents(el, index, array) {
                        if (scopeIdList.indexOf(el.eventId) == -1) {
                            $scope.events.push(el);
                        }
                    }

                    data.forEach(uploadEvents)
                }
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