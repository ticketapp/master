app.controller('phoneHomeCtrl', function ($scope, $rootScope, $http) {
    $scope.events = [];
    $scope.infos = [];
    $scope.time = 6;
    $scope.selectedTime = 6;
    $scope.zoom = 12;
    var time = 168;
    var changeTime = true;
    var offset = 0;
    var map;
    function getEvents () {
        if (time == 168) {
            $scope.map = false;
        }
        $http.get('/events/offsetAndMaxStartTime/'+ offset+ '/' + $rootScope.geoLoc + '/' + time).
            success(function (data, status, headers, config) {
                $scope.events = data;
                var eventsLength = $scope.events.length;
                console.log(eventsLength)
                for (var i = 0; i < eventsLength; i++) {
                    if ( $scope.events[i].places[0] != undefined) {
                        $scope.events[i].addresses[0].geographicPoint = $scope.events[i].places[0].geographicPoint.replace('(', '').replace(')', '');
                    }
                    if ($scope.events[i].startTime != undefined) {
                        $scope.events[i].countdown = Math.round(($scope.events[i].startTime - new Date()) / 3600000);
                        if (changeTime == true && $scope.events[i].addresses[0] != undefined && time == 168) {
                            if ($scope.selectedTime == 6 && $scope.events[i].countdown > $scope.selectedTime) {
                                $scope.selectedTime = $scope.events[i].countdown;
                            } else if ($scope.selectedTime != 6 && $scope.events[i].countdown < $scope.selectedTime && $scope.events[i].countdown > 6) {
                                $scope.selectedTime = $scope.events[i].countdown;
                            } else if ($scope.selectedTime != 6 && $scope.events[i].countdown < 6 && $scope.events[i].countdown > 0) {
                                $scope.selectedTime = 6;
                                changeTime = false;
                            }
                        }
                    }
                }
                $scope.map = true;
                $scope.updateMarkers()
            })
    }
    $scope.updateMarkers = function () {
        if ($scope.time > 23 && $scope.time <= 38) {
            $scope.selectedTime = ($scope.time-23)*24
        } else if ($scope.time > 38 && $scope.time <= 40) {
            $scope.selectedTime = ($scope.time-36)*168;
        } else if ($scope.time > 40) {
             $scope.selectedTime = ($scope.time-39)*720;
        } else {
            $scope.selectedTime = $scope.time;
        }
        $scope.dynMarkers = [];
        if ($scope.markerClusterer != undefined) {
            $scope.markerClusterer.clearMarkers();
        }
        var eventsLength = $scope.events.length;
        for (var i=0; i<eventsLength; i++) {
            if ($scope.events[i].countdown <= $scope.selectedTime && $scope.events[i].addresses[0] != undefined) {
                var geoPoint = $scope.events[i].addresses[0].geographicPoint;
                console.log(geoPoint.substring(0, geoPoint.indexOf(',')));
                console.log(geoPoint.replace(/^.+,/,''));
                var markerGenre;
                function pushMarker (markerGenre, geoPoint) {
                    var latLng = new google.maps.LatLng(geoPoint.substring(0, geoPoint.indexOf(',')), geoPoint.replace(/^.+,/,''));
                    $scope.dynMarkers.push(new google.maps.Marker({position: latLng, icon: '../assets/img/' + markerGenre}));
                }
                if ($scope.events[i].genres.length == 0) {
                    markerGenre = 'music187.png';
                } else {
                    var foundMarkerGenre = false;
                    for (var g = 0; g < $scope.events[i].genres.length; g++) {
                        console.log($scope.events[i].genres[g])
                        if ($scope.events[i].genres[g].name == 'electro' ||
                            $scope.events[i].genres[g].name == 'techno' ||
                            $scope.events[i].genres[g].name == 'd&b' ||
                            $scope.events[i].genres[g].name == 'drum' ||
                            $scope.events[i].genres[g].name == 'dubstep' ||
                            $scope.events[i].genres[g].name == 'trance' && foundMarkerGenre == false) {
                            console.log('yryr')
                            markerGenre = 'music200.png';
                            foundMarkerGenre = true;
                        } else if ($scope.events[i].genres[g].name.indexOf('rock') > -1 && foundMarkerGenre == false) {
                            markerGenre = 'rock3.png';
                            foundMarkerGenre = true;
                        } else if ($scope.events[i].genres[g].name.indexOf('reggae') > -1 ||
                            $scope.events[i].genres[g].name == 'dub' && foundMarkerGenre == false) {
                            markerGenre = 'jamaica_640.png';
                            foundMarkerGenre = true;
                        }
                        console.log(markerGenre)
                    }
                    if (foundMarkerGenre == false) {
                        markerGenre = 'music187.png';
                    }
                }
                console.log(markerGenre)
                pushMarker(markerGenre, geoPoint);
            }
        }
        $scope.markerClusterer = new MarkerClusterer(map, $scope.dynMarkers, {});
        $scope.markerClusterer.zoomOnClick_ = false;
        console.log($scope.markerClusterer)
        var markersLength = $scope.markerClusterer.markers_.length
        for (i = 0; i < markersLength; i++) {
            var marker = $scope.markerClusterer.markers_[i]
            google.maps.event.addListener(marker, 'click', function(marker) {
               console.log(marker)
                var redirectPath = ''
                var eventsLength = $scope.events.length;
                for (var i = 0; i < eventsLength; i++) {
                    if ($scope.events[i].countdown <= $scope.selectedTime &&
                        $scope.events[i].addresses[0] != undefined) {
                        var geopoint = $scope.events[i].addresses[0].geographicPoint
                        console.log(geopoint)
                        if (geopoint.substring(0, geopoint.indexOf(',')) < marker.latLng.k + 0.000001 &&
                            geopoint.substring(0, geopoint.indexOf(',')) > marker.latLng.k - 0.000001 &&
                            geopoint.replace(/^.+,/,'') < marker.latLng.D + 0.000001 &&
                            geopoint.replace(/^.+,/,'') > marker.latLng.D - 0.000001) {
                            console.log($scope.events[i])
                            redirectPath = 'event/' + $scope.events[i].eventId;
                        }
                    }
                }
                window.location.href =('#/' + redirectPath);
            });
        }
        google.maps.event.addListener($scope.markerClusterer, 'clusterclick', function(cluster) {
            console.log(cluster)
            var eventsLength = $scope.events.length;
            var redirectPath = '';
            for (var i = 0; i < eventsLength; i++) {
                if ($scope.events[i].addresses[0] != undefined &&
                    $scope.events[i].countdown <= $scope.selectedTime ) {
                    var geopoints = $scope.events[i].addresses[0].geographicPoint
                    console.log(geopoints.replace(/^.+,/,''))
                    console.log(cluster.center_.D)
                    if (geopoints.substring(0, geopoints.indexOf(',')) < cluster.center_.k + 0.000001 &&
                        geopoints.substring(0, geopoints.indexOf(',')) > cluster.center_.k - 0.000001 &&
                        geopoints.replace(/^.+,/,'') < cluster.center_.D + 0.000001 &&
                        geopoints.replace(/^.+,/,'') > cluster.center_.D - 0.000001) {
                        console.log($scope.events[i].places[0].placeId)
                        redirectPath = 'lieu/' + $scope.events[i].places[0].placeId;
                    }
                }
            }
            window.location.href =('#/' + redirectPath);
        })
        google.maps.event.addListener(map, 'zoom_changed', function() {
            // 3 seconds after the center of the map has changed, pan back to the
            // marker.
            if (map.zoom < $scope.zoom) {
                $scope.zoom = map.zoom;
                console.log($scope.zoom)
            }
        });
        if ($scope.selectedTime > time) {
            time = $scope.selectedTime;
            getEvents();
        }
    };
    $scope.$on('mapInitialized', function(event, evtMap) {
        map = evtMap;
        $scope.updateMarkers()
    });
    $scope.goTo = function (e, id) {
        var redirectPath = 'event/' + $scope.events[id].eventId;
        var eventsLength = $scope.events.length;
        for (var i = 0; i < eventsLength; i++) {
            if ($scope.events[i].countdown < $scope.selectedTime &&
                $scope.events[i].countdown > 0 &&
                i != id &&
                $scope.events[id].places[0] != undefined &&
                $scope.events[i].addresses[0] != undefined) {
                    if ($scope.events[i].addresses[0].geographicPoint == $scope.events[id].addresses[0].geographicPoint) {
                        console.log($scope.events[i])
                        console.log($scope.events[id])
                        redirectPath = 'lieu/' + $scope.events[id].places[0].placeId;
                    }
            }
        }
        window.location.href =('#/' + redirectPath);
    };
    $http.get('/infos').success(function (data, status, headers, config) {
        $scope.infos = data;
    });

    console.log($rootScope.geoLoc);
    if ($rootScope.geoLoc.length > 0) {
        getEvents ();
        $scope.mapCenter = $rootScope.geoLoc.replace('(', '');
        $scope.mapCenter = $scope.mapCenter.replace(')', '');
    } else {
        $rootScope.$watch('geoLoc', function () {
            getEvents ();
            $scope.mapCenter = $rootScope.geoLoc.replace('(', '');
            $scope.mapCenter = $scope.mapCenter.replace(')', '');
        })
    }
});