app.controller('iframeCtrl', function ($http, $scope, $rootScope, $timeout, $filter){
    var offset =  0;
    $scope.search = '';
    $scope.events = [];
    console.log($rootScope.geoLoc);
    $scope.getEvents = function () {
        if ($scope.search.length == 0) {
            $http.get('/events/nearGeoPoint/' + $rootScope.geoLoc + '/12/' + offset).
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
                                el.priceColor = 'rgb(' + tariffs[0] * 10 + ',' + (250 - (tariffs[0] * 10 ) ) +
                                    ',' + (175 - (tariffs[0] * 10 )) + ')'
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
        } else {
            $timeout(function() {
                $scope.$apply(function () {
                    $scope.events = $filter('filter')($scope.events, {name: $scope.search})
                    $scope.scopeIdList = [];
                })
            },0);
            $http.get('/events/containing/' + $scope.search + '/' + $rootScope.geoLoc).
                success(function (data, status, headers, config) {
                    function uploadEvents(el, index, array) {
                        $timeout(function() {
                            $scope.$apply(function () {
                                function getEventsId(el, index, array) {
                                    $scope.scopeIdList.push(el.eventId);
                                }
                                $scope.events.forEach(getEventsId);
                            });
                            if ($scope.scopeIdList.indexOf(el.eventId) == -1) {
                                el.priceColor = 'rgb(0, 140, 186)';
                                var placeLenght = el.places.length
                                for (var i = 0; i < placeLenght; i++) {
                                    if (el.places[i].geographicPoint != undefined) {
                                        el.places[i].geographicPoint = el.geographicPoint.replace("(", "");
                                        el.places[i].geographicPoint = el.geographicPoint.replace(")", "");
                                        el.places[i].geographicPoint = el.geographicPoint.replace(",", ", ");
                                    }
                                }
                                if (el.tariffRange != undefined) {
                                    var tariffs = el.tariffRange.split('-');
                                    if (tariffs[1] > tariffs[0]) {
                                        el.tariffRange = tariffs[0].replace('.0', '') + '€ - ' +
                                            tariffs[1].replace('.0', '') + '€';
                                    } else {
                                        el.tariffRange = tariffs[0].replace('.0', '') + '€';
                                    }
                                    el.priceColor = 'rgb(' + tariffs[0]*2 + ',' + (200 - (tariffs[0]*4 ) )+
                                        ',' + tariffs[0]*4 + ')'
                                }
                                $scope.events.push(el);
                                $scope.scopeIdList.push(el.eventId);
                                console.log($scope.scopeIdList)
                                console.log(el.eventId)
                            }
                        },0);
                    }
                    for (var i = 0; i < data.length; i++) {
                        if (data[i].name.toLowerCase().indexOf($scope.search.toLowerCase()) > -1) {
                            uploadEvents(data[i]);
                        }
                    }
                    $scope.loadingMore = false;
                    $http.get('/artists/containing/'+$scope.search).
                        success(function(data, status, headers, config) {
                            function getArtistEvents (art) {
                                $http.get('/artists/'+ art.facebookUrl + '/events ').
                                    success(function(data){
                                        data.forEach(uploadEvents);
                                        $rootScope.resizeImgHeight()
                                    })
                            }
                            data.forEach(getArtistEvents)
                            $scope.loadingMore = false;
                        });
                    $http.get('/genres/'+ $scope.search +'/20/' + offset + '/events ').
                        success(function(data, status, headers, config) {
                            data.forEach(uploadEvents);
                            $rootScope.resizeImgHeight()
                        });
                    $http.get('/places/containing/'+$scope.search).
                        success(function(data, status, headers, config) {
                            function getPlaceEvents (place) {
                                $http.get('/places/'+ place.placeId + '/events ').
                                    success(function(data){
                                        console.log(data)
                                        data.forEach(uploadEvents);
                                        $rootScope.resizeImgHeight()
                                    })
                            }
                            data.forEach(getPlaceEvents)
                        });
                    $http.get('/events/nearCity/' + $scope.search + '/12/' + offset ).
                        success(function (data) {
                            console.log(data)
                            data.forEach(uploadEvents);
                            $rootScope.resizeImgHeight()
                        });
                    $rootScope.resizeImgHeight()
                    $scope.loadingMore = false;
                }).
                error(function (data, status, headers, config) {
                    // called asynchronously if an error occurs
                    // or server returns response with an error status.
                });
        }
    }
    $scope.moreLimit = function () {
        offset = offset + 20;
        $rootScope.resizeImgHeight();
        getEvents();
    };
    $scope.getEvents();
    $rootScope.$watch('geoLoc', function (newval) {
        console.log(newval)
        $scope.getEvents();
    })
});