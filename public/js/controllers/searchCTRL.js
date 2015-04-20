app.controller('searchCtrl', ['$scope', '$http', '$rootScope', '$filter', 'oboe', '$timeout',
    'ArtistsFactory', 'EventsFactory', 'OrganizerFactory', 'PlaceFactory',
    function($rootScope, $http, $scope, $filter, oboe, $timeout, ArtistsFactory, EventsFactory, OrganizerFactory,
             PlaceFactory){
        $scope.limit = 12;
        $scope.artists = [];
        $scope.artistsFb = [];
        $scope.organizers = [];
        $scope.places = [];
        $scope.events = [];
        $scope.loadingMore = true;
        var offset = 0;
        var _selArtist = $rootScope.activArtist;
        var _selEvent = $rootScope.activEvent;
        var _selOrganizer = $rootScope.activUsr;
        var _selPlace = $rootScope.activPlace;
        var _selStart = ($rootScope.maxStart-23)*24;
        if ($rootScope.storeSearch != undefined && $rootScope.storeSearch.length > 0) {
            var _research = $rootScope.storeSearch;
            $rootScope.remStoreSearch();
        } else if (document.getElementById('searchBar') != null) {
            var _research = document.getElementById('searchBar').value.trim();
        } else {
            var _research = '';
        }
        function updateScope (data, scope, idName) {
            var scopeIdList = [];
            function getId(el, index, array) {
                var idDictionnary = {'artistId': el.artistId, 'eventId': el.eventId, 'organizerId': el.organizerId, 'placeId': el.placeId};
                scopeIdList.push(idDictionnary[idName]);
            }
            scope.forEach(getId);
            function pushEl (el, index, array) {
                var idDictionnary = {'artistId': el.artistId, 'eventId': el.eventId, 'organizerId': el.organizerId, 'placeId': el.placeId};
                if (scopeIdList.indexOf(idDictionnary[idName]) == -1) {
                    scope.push(el);
                }
            }
            data.forEach(pushEl);
            $rootScope.resizeImgHeight();
        }
        function colorEvent(el) {
            el.priceColor = 'rgb(0, 140, 186)';
            if (el.tariffRange != undefined) {
                var tariffs = el.tariffRange.split('-');
                if (tariffs[1] > tariffs[0]) {
                    el.tariffRange = tariffs[0].replace('.0', '') + ' - ' +
                        tariffs[1].replace('.0', '') + '€';
                } else {
                    el.tariffRange = tariffs[0].replace('.0', '') + '€';
                }
                el.priceColor = 'rgb(' + tariffs[0] * 2 + ',' + (200 - (tariffs[0] * 4 ) ) +
                    ',' + tariffs[0] * 4 + ')'
            }
        }

        function refactorGeopoint(el) {
            if (el.geographicPoint != undefined) {
                el.geographicPoint = el.geographicPoint.replace("(", "");
                el.geographicPoint = el.geographicPoint.replace(")", "");
                el.geographicPoint = el.geographicPoint.replace(",", ", ");
            }
        }

        function filterEventsByTime() {
            var eventsLenght = $scope.events.length;
            var maxStartTime = _selStart * 3600000 + new Date().getTime();
            for (var e = 0; e < eventsLenght; e++) {
                if ($scope.events[e].startTime > maxStartTime) {
                    $scope.events.splice(e, 1)
                    $scope.$apply();
                    e = e - 1;
                    eventsLenght = eventsLenght - 1;
                }
            }
        }
        function getArtists () {
            ArtistsFactory.getArtists(offset).then(function (artists) {
                updateScope(artists, $scope.artists, 'artistId');
                $scope.loadingMore = false;
            });
        }
        function getArtistsByGenre () {
            ArtistsFactory.getArtistsByGenre(offset, _research).then(function (artists) {
                updateScope(artists, $scope.artists, 'artistId');
            })
        }
        function getArtistsByContaining () {
            ArtistsFactory.getArtistsByContaining(_research).then(function (artists) {
                updateScope(artists, $scope.artists, 'artistId');
                $scope.loadingMore = false;
                getArtistsByGenre()
            });
        }

        function getEvents() {
            EventsFactory.getEvents(_selStart, $rootScope.geoLoc, offset).then(function (events) {
                events.forEach(colorEvent)
                updateScope(events, $scope.events, 'eventId');
                $scope.loadingMore = false;
            });
        }

        function getEventsByContaining() {
            EventsFactory.getEventsByContaining(_research, $rootScope.geoLoc).then(function (events) {
                events.forEach(colorEvent);
                updateScope(events, $scope.events, 'eventId');
                $scope.loadingMore = false;
            });
        }

        function getEventsArtistByContaining() {
            EventsFactory.getArtistsEventsByContaining(_research).then(function (events) {
                events.forEach(colorEvent);
                updateScope(events, $scope.events, 'eventId');
                $scope.loadingMore = false;
            });
        }

        function getEventsByGenre() {
            EventsFactory.getEventsByGenre(_research, offset).then(function (events) {
                events.forEach(colorEvent);
                updateScope(events, $scope.events, 'eventId');
                $scope.loadingMore = false;
            });
        }

        function getPlacesEventsByContaining() {
            EventsFactory.getPlacesEventsByContaining(_research).then(function (events) {
                events.forEach(colorEvent);
                updateScope(events, $scope.events, 'eventId');
                $scope.loadingMore = false;
            });
        }

        function getEventsByCity() {
            EventsFactory.getEventsByCity(_research, offset).then(function (events) {
                events.forEach(colorEvent);
                updateScope(events, $scope.events, 'eventId');
                $scope.loadingMore = false;
            });
        }

        function getOrganizersByContaining() {
            OrganizerFactory.getOrganizersByContaining(_research).then(function (organizers) {
                updateScope(organizers, $scope.organizers, 'organizerId');
                $scope.loadingMore = false;
            });
        }

        function getOrganizers() {
            OrganizerFactory.getOrganizers(offset).then(function (organizers) {
                updateScope(organizers, $scope.organizers, 'organizerId');
                $scope.loadingMore = false;
            });
        }

        function getPlaces() {
            PlaceFactory.getPlaces(offset, $rootScope.geoLoc).then(function (places) {
                places.forEach(refactorGeopoint)
                updateScope(places, $scope.places, 'placeId');
                $scope.loadingMore = false;
            });
        }

        function search () {
            if (_research.length == 0) {
                if (_selEvent == true) {
                    getEvents();
                }
                if (_selArtist == true) {
                    getArtists()
                }
                if (_selOrganizer == true) {
                    getOrganizers();
                }
                if (_selPlace == true) {
                    getPlaces();
                }
                $scope.artistsFb = [];
        } else {
            if (_selArtist == true) {
                if (_research != 'electro' &&
                    _research != 'reggae' &&
                    _research != 'rock' &&
                    _research != 'jazz' &&
                    _research != 'musique du monde' &&
                    _research != 'musique latine' &&
                    _research != 'classique' &&
                    _research != 'hip-hop' &&
                    _research != 'chanson' || offset == 0) {
                    $scope.artistsFb = $filter('filter')($scope.artistsFb, {name: _research});
                    $scope.artists = $filter('filter')($scope.artists, {name: _research});
                }
                getArtistsByContaining();
            }
            if (_selPlace == true) {
                $scope.places = $filter('filter')($scope.places, {name :  _research});
                $http.get('/places/containing/'+_research).
                    success(function(data, status, headers, config) {
                        var scopeIdList = [];
                        function getPlaceId(el, index, array) {
                            scopeIdList.push(el.placeId);
                        }
                        $scope.places.forEach(getPlaceId);
                            function uploadPlaces(el, index, array) {
                                if (scopeIdList.indexOf(el.placeId) == -1) {
                                    if (el.geographicPoint != undefined) {
                                        el.geographicPoint = el.geographicPoint.replace("(", "");
                                        el.geographicPoint = el.geographicPoint.replace(")", "");
                                        el.geographicPoint = el.geographicPoint.replace(",", ", ");
                                    }
                                    $scope.places.push(el);
                                }
                            }
                            data.forEach(uploadPlaces)
                        $http.get('/places/nearCity/' +  _research + '/12/' + offset).
                            success(function (data) {
                                data.forEach(uploadPlaces)
                            });
                        $rootScope.resizeImgHeight()
                        $scope.loadingMore = false;
                    }).
                    error(function(data, status, headers, config) {
                        // called asynchronously if an error occurs
                        // or server returns response with an error status.
                    });
            }
            if (_selOrganizer == true) {
                $scope.organizers = $filter('filter')($scope.organizers, {nickname :  _research});
                getOrganizersByContaining();
            }
            if (_selEvent == true) {
                if (_research != 'electro' &&
                    _research != 'reggae' &&
                    _research != 'rock' &&
                    _research != 'jazz' &&
                    _research != 'musique du monde' &&
                    _research != 'musique latine' &&
                    _research != 'classique' &&
                    _research != 'hip-hop' &&
                    _research != 'chanson' || offset == 0) {
                    $timeout(function () {
                        $scope.$apply(function () {
                            $scope.events = $filter('filter')($scope.events, {name: _research})
                        })
                    }, 0);
                }
                getEventsByContaining();
                getEventsArtistByContaining();
                getEventsByGenre();
                getPlacesEventsByContaining();
                getEventsByCity();
            }
        }
    }
    function searchArtistFb () {
        $scope.artistsFb = $filter('filter')($scope.artistsFb, {name :  _research});
        if ($scope.artistsFb.length < $scope.limit && _selArtist == true && _research.length > 1) {
            $scope.loadingFbArt = true;
            $http.get('/artists/facebookContaining/'+_research)
                .success(function (value) {
                    $scope.loadingFbArt = false;
                    var artistFbIdList = [];
                    function updateArtistFb (artistInfo) {
                        function getArtistFbIdInArtists (el) {
                            artistFbIdList.push(el.facebookId);
                        }
                        $scope.artists.forEach(getArtistFbIdInArtists);
                        $scope.artistsFb.forEach(getArtistFbIdInArtists);
                        if (artistFbIdList.indexOf(artistInfo.facebookId) < 0) {
                            artistInfo.tracks = [];
                            artistFbIdList.push(artistInfo.facebookId);
                            $scope.artistsFb.push(artistInfo);
                            $rootScope.resizeImgHeight();
                        }
                    }
                    value.forEach(updateArtistFb);
                })
                .error(function (error) {
                    $scope.loadingFbArt = false;

                });
        }
    }
    search();
    if (_selArtist == true && _research.length > 2) {
        searchArtistFb ();
    }
    $scope.initializeTime = function () {
        var newName = $rootScope.maxStart;
        if (newName > 23 && newName <= 38) {
            newName = (newName - 23) * 24
        } else if (newName > 38 && newName <= 40) {
            newName = (newName - 36) * 168;
        } else if (newName > 40) {
            newName = (newName - 39) * 720;
        }
        var waitForSearchBar = setInterval(function () {
            if ($rootScope.window == 'small' || $rootScope.window == 'medium') {
                var textSlider = document.getElementById('timeSearchSliderPhone').getElementsByClassName('md-thumb');
            } else {
                var slider = document.getElementsByClassName('bigSlider');
                var textSlider = [];
                for (var ii =0; ii < slider.length; ii++) {
                    textSlider.push(slider[ii].getElementsByClassName('md-thumb')[0]);
                }
            }
            if ($rootScope.path == 'search' || textSlider.length >= 2) {
                clearInterval(waitForSearchBar);
                for (var i = 0; i < textSlider.length; i++) {
                    textSlider[i].innerHTML = '';
                    textSlider[i].innerHTML = textSlider[i].innerHTML + '<b style="color: #ffffff">' +
                        $filter('millSecondsToTimeString')(newName) + '</b>';
                }
            } else if (_selEvent == false || _research.length > 0) {
                clearInterval(waitForSearchBar);
            }
        }, 100);
    }
    $scope.initializeTime();
    $scope.moreLimit = function () {
        offset = offset + 12;
        $scope.loadingMore = true;
        $rootScope.resizeImgHeight();
        search();
    };
    var typingTimer;
    var doneTypingInterval = 600;
    $scope.research = function(newName) {
        if (angular.isDefined(newName)) {
            _research = newName;
            $scope.searchPat = newName;
            $scope.limit = 12;
            offset = 0;
            if (_research.length == 0) {
                $scope.initializeTime()
            }
            if (_selArtist == true && _research.length > 2 &&
                _research != 'electro' &&
                _research != 'reggae' &&
                _research != 'rock' &&
                _research != 'jazz' &&
                _research != 'musique du monde' &&
                _research != 'musique latine' &&
                _research != 'classique' &&
                _research != 'hip-hop' &&
                _research != 'chanson'
                ) {
                $scope.artistsFb = $filter('filter')($scope.artistsFb, {name :  _research});
                clearTimeout(typingTimer);
                typingTimer = setTimeout(searchArtistFb, doneTypingInterval);
            }
            search();
        }
        return _research;
    };
    $scope.selArtist = function(newName) {
        if (angular.isDefined(newName)) {
            _selArtist = newName;
            search();
            searchArtistFb();
            $scope.limit = 12;
            offset = 0;
            if (newName == true) {
                $scope.loadingMore = true;
            } else {
                $scope.artists = []
            }
        }
        return _selArtist;
    };
    $scope.selEvent = function(newName) {
        if (angular.isDefined(newName)) {
            _selEvent = newName;
            search();
            $scope.limit = 12;
            offset = 0;
            if (newName == true) {
                $scope.loadingMore = true;
                $scope.initializeTime()
            } else {
                $scope.events = []
            }
        }
        return _selEvent;
    };
    $scope.selPlace = function(newName) {
        if (angular.isDefined(newName)) {
            _selPlace = newName;
            search();
            $scope.limit = 12;
            offset = 0;
            if (newName == true) {
                $scope.loadingMore = true;
            } else {
                $scope.places = []
            }
        }
        return _selPlace;
    };
    $scope.selUsr = function(newName) {
        if (angular.isDefined(newName)) {
            _selOrganizer = newName;
            search();
            $scope.limit = 12;
            offset = 0;
            if (newName == true) {
                $scope.loadingMore = true;
            } else {
                $scope.organizers = []
            }
        }
        return _selOrganizer;
    };
    var StartTimer;
    var doneStartInterval = 600;
    $scope.selStart = function(newName) {
        if (angular.isDefined(newName)) {
            $scope.limit = 12;
            offset = 0;
            if (newName > 23 && newName <= 38) {
                newName = (newName - 23) * 24
            } else if (newName > 38 && newName <= 40) {
                newName = (newName - 36) * 168;
            } else if (newName > 40) {
                newName = (newName - 39) * 720;
            }
            if ($rootScope.window == 'small' || $rootScope.window == 'medium') {
                var textSlider = document.getElementById('timeSearchSliderPhone').getElementsByClassName('md-thumb');
            } else {
                var slider = document.getElementsByClassName('bigSlider');
                var textSlider = [];
                    for (var ii =0; ii < slider.length; ii++) {
                        textSlider.push(slider[ii].getElementsByClassName('md-thumb')[0]);
                    }
            }
            for (var i = 0; i < textSlider.length; i++) {
                textSlider[i].innerHTML = '';
                textSlider[i].innerHTML = textSlider[i].innerHTML + '<b style="color: #ffffff">' +
                    $filter('millSecondsToTimeString')(newName) + '</b>';
            }
            _selStart = newName;
            clearTimeout(StartTimer);
            StartTimer = setTimeout(function () {
                search();
                filterEventsByTime();
            }, doneStartInterval);
            $scope.loadingMore = true;
            return _selStart;
        }
    };
}]);