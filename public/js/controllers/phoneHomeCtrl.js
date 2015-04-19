app.controller('phoneHomeCtrl', function ($scope, $rootScope, $http, $timeout, $filter) {
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
    $scope.getEvents = function () {
        var eventsLenghtForTime = $scope.events.length;
        var maxStartTime =  time*3600000 + new Date().getTime();
        for (var e = 0; e < eventsLenghtForTime; e++) {
            if ($scope.events[e].startTime > maxStartTime) {
                $scope.events.splice(e, 1)
                $scope.$apply();
                e = e -1;
                eventsLenghtForTime = eventsLenghtForTime - 1;
            }
        }
        $http.get('/events/inInterval/' + time + '/(' + $scope.mapCenter.replace(/^.+, /,'') + ',' +
            $scope.mapCenter.substring(0, $scope.mapCenter.indexOf(',')) + ')/20/'+ offset).
            success(function (data, status, headers, config) {
                var scopeIdList = [];
                function getEventId(el, index, array) {
                    scopeIdList.push(el.eventId);
                }
                $scope.events.forEach(getEventId);
                function uploadEvents(el, index, array) {
                    if (scopeIdList.indexOf(el.eventId) == -1) {
                        var placeLenght = el.places.length
                        for (var i = 0; i < placeLenght; i++) {
                            if (el.places[i].geographicPoint != undefined) {
                                el.places[i].geographicPoint = el.geographicPoint.replace("(", "");
                                el.places[i].geographicPoint = el.geographicPoint.replace(")", "");
                                el.places[i].geographicPoint = el.geographicPoint.replace(",", ", ");
                            }
                        }
                        $scope.events.push(el);
                    }
                }
                data.forEach(uploadEvents)
                var eventsLength = $scope.events.length;
                for (var i = 0; i < eventsLength; i++) {
                    if ( $scope.events[i].places[0] != undefined) {
                        $scope.events[i].addresses[0].geographicPoint = $scope.events[i].places[0].geographicPoint.replace('(', '').replace(')', '');
                        var geoPoint = $scope.events[i].addresses[0].geographicPoint;
                        console.log($scope.mapBounces)
                        if ($scope.mapBounces != undefined && geoPoint.substring(0, geoPoint.indexOf(',')) <= $scope.mapBounces.Da.j &&
                            geoPoint.substring(0, geoPoint.indexOf(',')) >= $scope.mapBounces.Da.k &&
                            geoPoint.replace(/^.+,/,'') <= $scope.mapBounces.va.k &&
                            geoPoint.replace(/^.+,/,'') >= $scope.mapBounces.va.j) {
                            eventInBounce = true;
                        }
                    }
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
            })
    }
    $scope.initializeTime = function () {
        var newName = $scope.time;
        if (newName > 23 && newName <= 38) {
            newName = (newName - 23) * 24
        } else if (newName > 38 && newName <= 40) {
            newName = (newName - 36) * 168;
        } else if (newName > 40) {
            newName = (newName - 39) * 720;
        }
        var textSlider = document.getElementById('timeSearchSliderPhone').getElementsByClassName('md-thumb');
        var waitForSearchBar = setInterval(function () {
            clearInterval(waitForSearchBar);
            for (var i = 0; i < textSlider.length; i++) {
                textSlider[i].innerHTML = '';
                textSlider[i].innerHTML = textSlider[i].innerHTML + '<b style="color: #ffffff">' +
                    $filter('millSecondsToTimeString')(newName) + '</b>';
            }
        }, 100);
    }
    $scope.updateMarkers = function () {
        $scope.dynMarkers = [];
        if ($scope.markerClusterer != undefined) {
            $scope.markerClusterer.clearMarkers();
        }
        var eventsLength = $scope.events.length;
        for (var i=0; i<eventsLength; i++) {
            if ($scope.events[i].addresses[0] != undefined) {
                var geoPoint = $scope.events[i].addresses[0].geographicPoint;
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
                        
                    }
                    if (foundMarkerGenre == false) {
                        markerGenre = 'music187.png';
                    }
                }
                pushMarker(markerGenre, geoPoint);
            }
        }
        $scope.markerClusterer = new MarkerClusterer(map, $scope.dynMarkers, {});
        $scope.markerClusterer.zoomOnClick_ = false;
        var markersLength = $scope.markerClusterer.markers_.length
        for (i = 0; i < markersLength; i++) {
            var marker = $scope.markerClusterer.markers_[i]
            google.maps.event.addListener(marker, 'click', function(marker) {
                var redirectPath = ''
                var eventsLength = $scope.events.length;
                for (var i = 0; i < eventsLength; i++) {
                    if ($scope.events[i].addresses[0] != undefined) {
                        var geopoint = $scope.events[i].addresses[0].geographicPoint
                        
                        if (geopoint.substring(0, geopoint.indexOf(',')) < marker.latLng.k + 0.000001 &&
                            geopoint.substring(0, geopoint.indexOf(',')) > marker.latLng.k - 0.000001 &&
                            geopoint.replace(/^.+,/,'') < marker.latLng.D + 0.000001 &&
                            geopoint.replace(/^.+,/,'') > marker.latLng.D - 0.000001) {
                            redirectPath = 'event/' + $scope.events[i].eventId;
                        }
                    }
                }
                window.location.href =('#/' + redirectPath);
            });
        }
        google.maps.event.addListener($scope.markerClusterer, 'clusterclick', function(cluster) {
            
            var eventsLength = $scope.events.length;
            var redirectPath = '';
            var count =0;
            var places = '';
            for (var i = 0; i < eventsLength; i++) {
                if ($scope.events[i].addresses[0] != undefined) {
                    var geopoints = $scope.events[i].addresses[0].geographicPoint
                    var markerLength = cluster.markers_.length;
                    for (var m = 0; m < markerLength; m++) {
                        if (geopoints.substring(0, geopoints.indexOf(',')) < cluster.markers_[m].position.k + 0.000001 &&
                            geopoints.substring(0, geopoints.indexOf(',')) > cluster.markers_[m].position.k - 0.000001 &&
                            geopoints.replace(/^.+,/, '') < cluster.markers_[m].position.D + 0.000001 &&
                            geopoints.replace(/^.+,/, '') > cluster.markers_[m].position.D - 0.000001) {
                            if (places.indexOf($scope.events[i].places[0].placeId) == -1) {
                                places = places + ', ' + $scope.events[i].places[0].placeId;
                                count = count + 1;
                            }
                        }
                    }
                }
            }
            if (count == 1) {
                redirectPath = 'lieu/' + places.replace(', ', '');
            } else {
                $timeout(function () {
                    $scope.$apply(function () {
                        $scope.mapCenter = cluster.center_.k + ',' + cluster.center_.D;
                        $scope.zoom = 14;
                    })
                }, 0)
            }
            window.location.href =('#/' + redirectPath);
        });
    };
    var StartTimer;
    var doneStartInterval = 600;
    $scope.timeChange = function () {
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
        clearTimeout(StartTimer)
        StartTimer = setTimeout($scope.getEvents, doneStartInterval);
    }
    $scope.goTo = function (e, id) {
        var redirectPath = 'event/' + $scope.events[id].eventId;
        var eventsLength = $scope.events.length;
        for (var i = 0; i < eventsLength; i++) {
            if (i != id &&
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
    function getBounds () {
        $scope.initializeTime();
        var waitForMap = setInterval(function () {
            if (document.getElementsByTagName('map').length > 0) {
                clearInterval(waitForMap)
                google.maps.event.addListener(map, 'zoom_changed', function() {
                    $scope.mapBounces = map.getBounds()
                    if (map.zoom < $scope.zoom) {
                        $scope.zoom = map.zoom;
                        offset = offset + 20;
                        $scope.getEvents();
                    }
                });
                google.maps.event.addListener(map, 'center_changed', function() {
                    $scope.mapBounces = map.getBounds()
                    $scope.mapCenter = map.center;
                })
                $scope.mapBounces = map.getBounds()
                $scope.getEvents()
            }
        }, 500)
    }
    if ($rootScope.geoLoc.length > 0) {
        $timeout(function () {
            $scope.$apply(function() {
                $scope.mapCenter = $rootScope.geoLoc.replace('(', '');
                $scope.mapCenter = $scope.mapCenter.replace(')', '');
                $scope.mapCenter = $scope.mapCenter.replace(",", ", ");
            })
            $scope.map = true;
            $scope.$on('mapInitialized', function(event, evmap) {
                map = evmap;
                getBounds(evmap)
            })
        },0)
    } else {
        $rootScope.$watch('geoLoc', function (newVal) {
            if (newVal.length > 0) {
                $timeout(function () {
                    $scope.$apply(function () {
                        $scope.mapCenter = newVal.replace('(', '');
                        $scope.mapCenter = $scope.mapCenter.replace(')', '');
                        $scope.mapCenter = $scope.mapCenter.replace(",", ", ");
                    })
                    $scope.map = true;
                    $scope.$on('mapInitialized', function (event, evmap) {
                        map = evmap;
                        getBounds(evmap)
                    })
                }, 0)
            }
        })
    }
});