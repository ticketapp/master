angular.module('claudeApp').controller('SmallHomeCtrl', ['$scope', '$rootScope', '$http', '$timeout', '$filter',
    'GeolocFactory', 'RefactorGeopoint', 'EventsFactory', 'NgMap', 'SearchFactory',
    function ($scope, $rootScope, $http, $timeout, $filter, GeolocFactory, RefactorGeopoint,
          EventsFactory, NgMap, SearchFactory) {
        $scope.events = [];
        $scope.infos = [];
        $scope.time = 6;
        $scope.selectedTime = 6;
        $scope.zoom = 12;
        var mapBounces;
        var time = 6;
        var offset = 0;
        var map;

        function filterIfIsNotOnTheMap(event) {
            if (event.geographicPoint) {
                var lats = mapBounces[Object.keys(mapBounces)[0]];
                lats = [lats[Object.keys(lats)[0]], lats[Object.keys(lats)[1]]];
                var lngs = mapBounces[Object.keys(mapBounces)[1]];
                lngs = [lngs[Object.keys(lngs)[0]], lngs[Object.keys(lngs)[1]]];
                var lat = event.geographicPoint.substring(0, event.geographicPoint.indexOf(','));
                var lng = event.geographicPoint.replace(/^.+,/, '');
                return lat <= lats[1] && lat >= lats[0] && lng <= lngs[1] && lng >= lngs[0]
            } else {
                return false
            }
        }

        function filterEventWithAddress (event) {
                return event.addresses.length > 0
        }

        function addEventsToScope (events) {
            events.filter(function(event) {
                return $scope.events.filter(function(scopeEvent) {
                    return scopeEvent.id === event.id
                }).length === 0;
            }).map(function(event) {
                $scope.events.push(event)
            })
        }

        function filterByEventsScope (marker) {
            return $scope.events.filter(function (event) {
                    return event.id === marker.id
                }).length > 0
        }

        function searchMoreEvents() {
            $scope.searchEventFirst = true;
            if ($scope.time < 24) {
                $scope.time = $scope.time + 6;
            } else if ($scope.time < 30) {
                $scope.time = $scope.time + 1;
            } else {
                $scope.zoom--
            }
            $scope.timeChange();
        }

        function updateScopes(events) {
            if (!mapBounces) {
                mapBounces = map.getBounds();
            }
            var filteredEvents = events.filter(filterEventWithAddress).filter(filterIfIsNotOnTheMap);
            addEventsToScope(filteredEvents);
            if ($scope.dynMarkers) {
                $scope.dynMarkers = $scope.dynMarkers.filter(filterByEventsScope);
            }

            if ($scope.events.length === 0) {
                searchMoreEvents();
            } else {
                $scope.searchEventFirst = false;
                updateMarkers()
            }
        }

        function getEvents () {
            var mapCenter = $scope.mapCenter.split(',');
            SearchFactory.getEvents(time, offset, mapCenter[1] + ',' + mapCenter[0]).
                then(function (events) {
                    updateScopes(events);
                });
        }

        function setNewTimeToSlider(newTime) {
            var waitForSearchBar = setInterval(function () {
                var textSlider = document.getElementsByClassName('md-thumb');
                if (textSlider !== undefined) {
                    clearInterval(waitForSearchBar);
                    for (var i = 0; i < textSlider.length; i++) {
                        textSlider[i].innerHTML = '';
                        textSlider[i].innerHTML = textSlider[i].innerHTML + '<b style="color: #ffffff">' +
                            $filter('millSecondsToTimeString')(newTime) + '</b>';
                    }
                }
            }, 100);
        }

        function convertTime(time) {
            var newTime = time;
            if (newTime > 23 && newTime <= 38) {
                newTime = (newTime - 23) * 24
            } else if (newTime > 38 && newTime <= 40) {
                newTime = (newTime - 36) * 168;
            } else if (newTime > 40) {
                newTime = (newTime - 39) * 720;
            }
            return newTime;
        }

        function initializeTimeSlider () {
            var newTime = convertTime($scope.time);
            setNewTimeToSlider(newTime);
        }

        function pushMarker (markerGenre, geoPoint, id, placeId) {
            var latLng = new google.maps.LatLng(geoPoint.substring(0, geoPoint.indexOf(',')), geoPoint.replace(/^.+,/,''));
            $scope.dynMarkers.push(new google.maps.Marker({position: latLng,
                icon: {url: '../assets/images/' + markerGenre,
                    scaledSize: new google.maps.Size(50, 50)}, id: id, placeId: placeId}));
        }

        function setIcon(iconChar) {
            if (iconChar === 'e' ) {
                return 'electro.png';
            } else if (iconChar === 'r' ) {
                return 'rock.png';
            } else if (iconChar === 'g' ) {
                return 'rasta.png';
            } else if (iconChar === 'h' ) {
                return 'rap.png';
            } else if (iconChar === 'j' ) {
                return 'jazz.png';
            } else if (iconChar === 'c' ) {
                return 'accoustique.png';
            } else if (iconChar === 'l' ) {
                return 'salsa.png';
            } else if (iconChar === 'm' ) {
                return 'du%20monde.png';
            } else if (iconChar === 's' ) {
                return 'classique.png';
            } else {
                return 'autres.png';
            }
        }

        function addIcon(event) {
            var markerGenre;
            if (event.genres.length == 0) {
                markerGenre = 'autres.png';
            } else {
                var icons = event.genres.map(function(genre) {
                    return genre.icon
                });
                var counter=[];
                var bestIcon='';
                var maxi = 0;
                for(var icon in icons) {
                    if (icon !== 'a') {
                        if (counter[icon]) {
                            counter[icon]++
                        } else {
                            counter[icon] = 1
                        }
                        if (maxi < counter[icon]) {
                            bestIcon = icon;
                            maxi = counter[icon]
                        }
                    }
                }
                markerGenre = setIcon(bestIcon)

            }
            return markerGenre;
        }

        function updateMarkers () {
            $scope.dynMarkers = [];
            if ($scope.markerClusterer) {
                $scope.markerClusterer.clearMarkers();
            }

            $scope.events.map(function(event) {
                pushMarker(addIcon(event), event.geographicPoint, event.id, event.places[0].id);
            });

            $scope.dynMarkers.map(function (marker) {
                marker.addListener('click', function() {
                    window.location.href =('#/events/' + marker.id);
                });
            });

            $scope.markerClusterer = new MarkerClusterer(map, $scope.dynMarkers, {});

            google.maps.event.addListener($scope.markerClusterer, 'clusterclick', function(cluster) {
                var places = [];
                var positions = [];
                var markersLength = cluster.markers_.length;
                for (var i = 0; i < markersLength; i ++) {
                    if (places.indexOf(cluster.markers_[i].placeId) === -1) {
                        places.push(cluster.markers_[i].placeId)
                    }
                    var position = cluster.markers_[i].position.lat() + " - " + cluster.markers_[i].position.lng();
                    if (positions.indexOf(position) < 0) {
                        positions.push(position)
                    }
                }
                if (places.length === 1 || positions.length === 1) {
                    window.location.href = ('#/places/' + places[0]);
                }
            });
        }

        var StartTimer;
        var doneStartInterval = 600;
        $scope.timeChange = function () {
            offset = 0;
            $scope.selectedTime = convertTime($scope.time);
            time = $scope.selectedTime;
            setNewTimeToSlider($scope.selectedTime);
            clearTimeout(StartTimer);
            StartTimer = setTimeout(getEvents, doneStartInterval);
        };

        function getBounds () {
            initializeTimeSlider();
            var waitForMap = setInterval(function () {
                if (document.getElementsByTagName('map').length > 0) {
                    clearInterval(waitForMap);
                    google.maps.event.addListener(map, 'zoom_changed', function() {
                        mapBounces = map.getBounds();
                        if (map.zoom < $scope.zoom) {
                            $scope.zoom = map.zoom;
                            offset = offset + 20;
                            getEvents();
                        }
                    });

                    google.maps.event.addListener(map, 'center_changed', function() {
                        mapBounces = map.getBounds();
                        $scope.mapCenter = map.getCenter().lat() + ', ' + map.getCenter().lng();
                    });
                    mapBounces = map.getBounds();
                    getEvents();
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
            }, 0)
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
