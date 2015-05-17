angular.module('claudeApp').controller('SmallHomeCtrl', ['$scope', '$rootScope', '$http',
    '$timeout', '$filter', 'GeolocFactory', 'RefactorGeopoint', 'EventsFactory',
    function ($scope, $rootScope, $http, $timeout, $filter, GeolocFactory, RefactorGeopoint,
              EventsFactory) {
    $scope.events = [];
    $scope.infos = [];
    $scope.time = 6;
    $scope.selectedTime = 6;
    $scope.zoom = 12;
    $scope.mapBounces;
    var eventInBounce = false;
    var time = 6;
    var offset = 0;
    var map;
    var firstShow = true;

    function uploadEvents(el, index, array) {
        var scopeIdList = [];
        function getEventId(el) {
            scopeIdList.push(el.eventId);
        }
        $scope.events.forEach(getEventId);
        if ($scope.mapBounces == undefined) {
            $scope.mapBounces = map.getBounds()
        }
        if (scopeIdList.indexOf(el.eventId) == -1) {
            if ( el.places[0] != undefined) {
                el.addresses[0].geographicPoint = el.places[0].geographicPoint.replace('(', '').replace(')', '');
                var geoPoint = el.addresses[0].geographicPoint;
                var firstObject = $scope.mapBounces[Object.keys($scope.mapBounces)[0]];
                var secondObject = $scope.mapBounces[Object.keys($scope.mapBounces)[1]];
                if ($scope.mapBounces != undefined) {
                    if (geoPoint.substring(0, geoPoint.indexOf(',')) <= firstObject[Object.keys(firstObject)[1]] &&
                        geoPoint.substring(0, geoPoint.indexOf(',')) >= firstObject[Object.keys(firstObject)[0]] &&
                        geoPoint.replace(/^.+,/, '') <= secondObject[Object.keys(secondObject)[1]] &&
                        geoPoint.replace(/^.+,/, '') >= secondObject[Object.keys(secondObject)[0]]) {
                        eventInBounce = true;
                    }
                }
            }
            $scope.events.push(el);
        }
    }

    $scope.getEvents = function () {
        var eventsLengthForTime = $scope.events.length;
        var maxStartTime =  time*3600000 + new Date().getTime();
        for (var e = 0; e < eventsLengthForTime; e++) {
            if ($scope.events[e].startTime > maxStartTime) {
                $timeout(function () {
                    $scope.$apply(function () {
                        $scope.events.splice(e, 1);
                    })
                }, 0);
                e = e -1;
                eventsLengthForTime = eventsLengthForTime - 1;
            }
        }
        console.log($scope.mapCenter);
        EventsFactory.getEvents(time, '(' + $scope.mapCenter.replace(/^.+, /,'') + ',' +
            $scope.mapCenter.substring(0, $scope.mapCenter.indexOf(',')) + ')', offset).
            then(function (events) {
            events.forEach(uploadEvents);
            if (eventInBounce == false && firstShow == true) {
                $scope.searchEventFirst = true;
                if ($scope.time < 24) {
                    $scope.time = $scope.time + 6;
                } else if ($scope.time < 30) {
                    $scope.time = $scope.time + 1;
                } else {
                    $scope.zoom --
                }
                $scope.timeChange();

            } else {
                firstShow = false;
                $scope.searchEventFirst = false;
                $scope.updateMarkers()
            }
        });
    };

    $scope.initializeTime = function () {
        var newName = $scope.time;
        if (newName > 23 && newName <= 38) {
            newName = (newName - 23) * 24
        } else if (newName > 38 && newName <= 40) {
            newName = (newName - 36) * 168;
        } else if (newName > 40) {
            newName = (newName - 39) * 720;
        }
        var waitForSearchBar = setInterval(function () {
            var textSlider = document.getElementsByClassName('md-thumb');
            if (textSlider != undefined) {
                clearInterval(waitForSearchBar);
                for (var i = 0; i < textSlider.length; i++) {
                    textSlider[i].innerHTML = '';
                    textSlider[i].innerHTML = textSlider[i].innerHTML + '<b style="color: #ffffff">' +
                        $filter('millSecondsToTimeString')(newName) + '</b>';
                }
            }
        }, 100);
    };

    function pushMarker (markerGenre, geoPoint, id, placeId) {
        var latLng = new google.maps.LatLng(geoPoint.substring(0, geoPoint.indexOf(',')), geoPoint.replace(/^.+,/,''));
        $scope.dynMarkers.push(new google.maps.Marker({position: latLng,
            icon: {url: '../assets/images/' + markerGenre,
                scaledSize: new google.maps.Size(50, 50)}, id: id, placeId: placeId}));
        console.log($scope.dynMarkers)
    }

    function addIcon(i) {
        var geoPoint = $scope.events[i].addresses[0].geographicPoint;
        var markerGenre;
        if ($scope.events[i].genres.length == 0) {
            markerGenre = 'autres.png';
        } else {
            var foundMarkerGenre = false;
            for (var g = 0; g < $scope.events[i].genres.length; g++) {
                if ($scope.events[i].genres[g].name == 'electro' && foundMarkerGenre == false) {
                    markerGenre = 'electro.png';
                    foundMarkerGenre = true;
                } else if ($scope.events[i].genres[g].name == 'rock' && foundMarkerGenre == false) {
                    markerGenre = 'rock.png';
                    foundMarkerGenre = true;
                } else if ($scope.events[i].genres[g].name == 'reggae' && foundMarkerGenre == false) {
                    markerGenre = 'rasta.png';
                    foundMarkerGenre = true;
                } else if ($scope.events[i].genres[g].name == 'hip-hop' && foundMarkerGenre == false) {
                    markerGenre = 'rap.png';
                    foundMarkerGenre = true;
                } else if ($scope.events[i].genres[g].name == 'jazz' && foundMarkerGenre == false) {
                    markerGenre = 'jazz.png';
                    foundMarkerGenre = true;
                } else if ($scope.events[i].genres[g].name == 'chanson' && foundMarkerGenre == false) {
                    markerGenre = 'accoustique.png';
                    foundMarkerGenre = true;
                }

            }
            if (foundMarkerGenre == false) {
                markerGenre = 'autres.png';
            }
        }
        return {geoPoint: geoPoint, markerGenre: markerGenre};
    }

    $scope.updateMarkers = function () {
        $scope.dynMarkers = [];
        if ($scope.markerClusterer != undefined) {
            $scope.markerClusterer.clearMarkers();
        }
        var eventsLength = $scope.events.length;
        for (var i=0; i<eventsLength; i++) {
            if ($scope.events[i].addresses[0] != undefined) {
                var __ret = addIcon(i);
                var geoPoint = __ret.geoPoint;
                var markerGenre = __ret.markerGenre;
                pushMarker(markerGenre, geoPoint, $scope.events[i].eventId,
                    $scope.events[i].places[0].placeId);
            }
        }
        $scope.markerClusterer = new MarkerClusterer(map, $scope.dynMarkers, {});
        //$scope.markerClusterer.zoomOnClick_ = false;
        var markersLength = $scope.dynMarkers.length;
        for (i = 0; i < markersLength; i++) {
            var marker = $scope.dynMarkers[i];
            var id = $scope.dynMarkers[i].id;
            marker.addListener('click', function() {
                window.location.href =('#/events/' + id);
            });
        }
        google.maps.event.addListener($scope.markerClusterer, 'clusterclick', function(cluster) {
            console.log(cluster);
            var places = [];
            var markersLength = cluster.markerClusterer_.markers_.length;
            for (i = 0; i < markersLength; i ++) {
                if (places.indexOf(cluster.markerClusterer_.markers_[i].placeId) == -1) {
                    places.push(cluster.markerClusterer_.markers_[i].placeId)
                }
            };
            if (places.length == 1) {
                window.location.href =('#/places/' + places[0]);
            }
        });
    };
    var StartTimer;
    var doneStartInterval = 600;
    $scope.timeChange = function () {
        offset = 0;
        if ($scope.time > 23 && $scope.time <= 38) {
            $scope.selectedTime = ($scope.time-23)*24
        } else if ($scope.time > 38 && $scope.time <= 40) {
            $scope.selectedTime = ($scope.time-36)*168;
        } else if ($scope.time > 40) {
            $scope.selectedTime = ($scope.time-39)*720;
        } else {
            $scope.selectedTime = $scope.time;
        }
        time = $scope.selectedTime;
        var textSlider = document.getElementsByClassName('md-thumb');
        for (var i = 0; i < textSlider.length; i++) {
            textSlider[i].innerHTML = '';
            textSlider[i].innerHTML = textSlider[i].innerHTML + '<b style="color: #ffffff">' +
                $filter('millSecondsToTimeString')($scope.selectedTime) + '</b>';
        }
        clearTimeout(StartTimer);
        StartTimer = setTimeout($scope.getEvents, doneStartInterval);
    };
    function getBounds () {
        $scope.initializeTime();
        var waitForMap = setInterval(function () {
            if (document.getElementsByTagName('map').length > 0) {
                clearInterval(waitForMap);
                google.maps.event.addListener(map, 'zoom_changed', function() {
                    $scope.mapBounces = map.getBounds();
                    if (map.zoom < $scope.zoom) {
                        $scope.zoom = map.zoom;
                        offset = offset + 20;
                        $scope.getEvents();
                    }
                });
                google.maps.event.addListener(map, 'center_changed', function() {
                    $scope.mapBounces = map.getBounds();
                    $scope.mapCenter = map.center[Object.keys(map.center)[0]] + ', ' + map.center[Object.keys(map.center)[1]];
                });
                $scope.mapBounces = map.getBounds();
                $scope.getEvents();
            }
        }, 500)
    }
    if ($rootScope.geoLoc.length > 0) {
        $timeout(function () {
            $scope.$apply(function() {
                $scope.mapCenter = RefactorGeopoint.refactorGeopoint($rootScope.geoLoc);
                $scope.map = true;
            });
            $scope.$on('mapInitialized', function(event, evmap) {
                map = evmap;
                getBounds(evmap)
            })
        },0)
    } else {
        $rootScope.$watch('geoLoc', function (newVal) {
            console.log($rootScope.geoloc)
            if (newVal.length > 0) {
                $timeout(function () {
                    $scope.$apply(function () {
                        $scope.mapCenter = RefactorGeopoint.refactorGeopoint($rootScope.geoLoc);
                        $scope.map = true;
                    });
                    $scope.$on('mapInitialized', function (event, evmap) {
                        map = evmap;
                        getBounds(evmap)
                    })
                }, 0)
            }
        })
    }
}]);