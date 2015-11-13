angular.module('claudeApp').controller('SmallHomeCtrl', ['$scope', '$rootScope', '$http',
    '$timeout', '$filter', 'GeolocFactory', 'RefactorGeopoint', 'EventsFactory', 'NgMap',
    function ($scope, $rootScope, $http, $timeout, $filter, GeolocFactory, RefactorGeopoint,
              EventsFactory, NgMap) {
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
            scopeIdList.push(el.id);
        }
        $scope.events.forEach(getEventId);
        if ($scope.mapBounces == undefined) {
            $scope.mapBounces = map.getBounds()
        }
        if (scopeIdList.indexOf(el.id) == -1) {
            if ( el.geographicPoint != undefined) {
                var geoPoint = el.geographicPoint;
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
                $scope.events.push(el);
            }
        }
    }

    $scope.getEvents = function () {
        var mapCenter = $scope.mapCenter.split(',');
        EventsFactory.getEvents(time,  mapCenter[1] + ',' + mapCenter[0], offset).
            then(function (events) {
            events.forEach(uploadEvents);
            var maxStartTime =  time*3600000 + new Date().getTime();
            $scope.events = $scope.events.filter(function(event) {
                return event.startTime <= maxStartTime
            });
            if ($scope.dynMarkers) {
                $scope.dynMarkers = $scope.dynMarkers.filter(function (marker) {
                    return $scope.events.filter(function (event) {
                            return event.id === marker.id
                        }).length > 0
                });
            }
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
    }

    function addIcon(i) {
        var geoPoint = $scope.events[i].geographicPoint;
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
                } else if ($scope.events[i].genres[g].name == 'musiques latines' && foundMarkerGenre == false) {
                    markerGenre = 'salsa.png';
                    foundMarkerGenre = true;
                } else if ($scope.events[i].genres[g].name == 'musiques du monde' && foundMarkerGenre == false) {
                    markerGenre = 'du%20monde.png';
                    foundMarkerGenre = true;
                } else if ($scope.events[i].genres[g].name == 'classique' && foundMarkerGenre == false) {
                    markerGenre = 'classique.png';
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
        if ($scope.markerClusterer) {
            $scope.markerClusterer.clearMarkers();
        }
        var eventsLength = $scope.events.length;
        for (var i=0; i<eventsLength; i++) {
            if ($scope.events[i].addresses[0] != undefined) {
                var __ret = addIcon(i);
                var geoPoint = __ret.geoPoint;
                var markerGenre = __ret.markerGenre;
                pushMarker(markerGenre, geoPoint, $scope.events[i].id,
                    $scope.events[i].places[0].id);
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
            var places = [];
            var markersLength = cluster.markerClusterer_.markers_.length;
            for (i = 0; i < markersLength; i ++) {
                if (places.indexOf(cluster.markerClusterer_.markers_[i].placeId) == -1) {
                    places.push(cluster.markerClusterer_.markers_[i].placeId)
                }
            }
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
                    $scope.mapCenter = map.getCenter().lat() + ', ' + map.getCenter().lng();
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
            NgMap.getMap().then(function(repMap) {
                map = repMap;
                getBounds(repMap)
            });
            $scope.$on('mapInitialized', function(event, evmap) {
                map = evmap;
                getBounds(evmap)
            })
        },0)
    } else {
        $rootScope.$watch('geoLoc', function (newVal) {
            if (newVal.length > 0 && newVal !== undefined) {
                $timeout(function () {
                    $scope.$apply(function () {
                        $scope.mapCenter = RefactorGeopoint.refactorGeopoint(newVal);
                        $scope.map = true;
                    });
                    NgMap.getMap().then(function(repMap) {
                        map = repMap;
                        getBounds(repMap)
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